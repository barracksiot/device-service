/*
 * MIT License
 *
 * Copyright (c) 2017 Barracks Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.barracks.deviceservice.repository;

import io.barracks.deviceservice.model.DataSet;
import io.barracks.deviceservice.model.Device;
import io.barracks.deviceservice.model.DeviceEvent;
import io.barracks.deviceservice.model.operator.Operator;
import io.barracks.deviceservice.model.operator.comparison.EqualOperator;
import io.barracks.deviceservice.model.operator.comparison.GreaterThanOperator;
import io.barracks.deviceservice.model.operator.logical.AndOperator;
import io.barracks.deviceservice.utils.DeviceUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.barracks.deviceservice.utils.DeviceEventUtils.getDeviceEvent;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@BarracksRepositoryTest
public class DeviceRepositoryTest {
    @Autowired
    private DeviceRepository deviceRepository;

    @Before
    public void setup() {
        deviceRepository.deleteAll();
    }

    @Test
    public void updateLastEvent_whenDeviceWasNotRegistered_shouldCreateDeviceWithFirstSeenAndEvent() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceEvent event = getDeviceEvent(userId, unitId);

        // When
        final boolean isNew = deviceRepository.updateLastEvent(userId, unitId, event);
        final Optional<Device> result = deviceRepository.findByUserIdAndUnitId(userId, unitId);

        // Then
        assertThat(result).isNotEmpty().hasValueSatisfying(device -> {
            assertThat(device.getFirstSeen()).isPresent();
            assertThat(device.getLastEvent()).isPresent().contains(event);
        });
        assertThat(isNew).isEqualTo(true);
    }

    @Test
    public void updateLastEvent_whenDeviceWasRegisteredAndHasNoEvent_shouldUpdateEvent() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Device device = deviceRepository.insert(DeviceUtils.getDevice(userId, unitId).toBuilder().lastEvent(null).build());
        final DeviceEvent event = getDeviceEvent(userId, unitId);
        final Device expected = device.toBuilder().lastEvent(event).build();

        // When
        final boolean isNew = deviceRepository.updateLastEvent(userId, unitId, event);
        final Optional<Device> result = deviceRepository.findByUserIdAndUnitId(userId, unitId);

        // Then
        assertThat(result).isPresent().contains(expected);
        assertThat(isNew).isFalse();
    }

    @Test
    public void updateLastEvent_whenDeviceWasRegisteredAndLastEventIsOlder_shouldNotUpdateEvent() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Device device = deviceRepository.insert(DeviceUtils.getDevice(userId, unitId));
        final DeviceEvent event = getDeviceEvent(userId, unitId);
        final Device expected = device.toBuilder().lastEvent(event).build();

        // When
        final boolean isNew = deviceRepository.updateLastEvent(userId, unitId, event);
        final Optional<Device> result = deviceRepository.findByUserIdAndUnitId(userId, unitId);

        // Then
        assertThat(result).isPresent().contains(expected);
        assertThat(isNew).isFalse();
    }

    @Test
    public void updateLastEvent_whenDeviceWasRegisteredAndLastEventIsMoreRecent_shouldNotUpdateEvent() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceEvent event = getDeviceEvent(userId, unitId);
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final Device device = DeviceUtils.getDevice(userId, unitId);
        deviceRepository.insert(device);

        // When
        final boolean isNew = deviceRepository.updateLastEvent(userId, unitId, event);
        final Optional<Device> result = deviceRepository.findByUserIdAndUnitId(userId, unitId);

        // Then
        assertThat(result).isPresent().contains(device);
        assertThat(isNew).isFalse();
    }

    @Test
    public void findByUserId_WhenNoQueryGivenAndNoDevice_shouldReturnEmptyPage() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);

        // When
        final Page<Device> result = deviceRepository.findByUserId(userId, null, pageable);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void findByUserId_WhenNoQueryGiven_shouldReturnDevicesThatBelongToUserId() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 50);
        final List<Device> userDevices = getDevicesForUser(userId, 10);
        final List<Device> otherDevices = getDevicesForUser("anotherUser", 10);
        final List<Device> allDevices = new ArrayList<Device>() {{
            addAll(userDevices);
            addAll(otherDevices);
        }};
        allDevices.forEach(device -> deviceRepository.save(device));

        // When
        final Page<Device> result = deviceRepository.findByUserId(userId, null, pageable);

        // Then
        assertThat(result).containsOnlyElementsOf(userDevices);
    }

    @Test
    public void findByUserId_WhenValidQueryGivenAndNoDevice_shouldReturnDevicesEmptyPage() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Operator searchFilter = new AndOperator(new ArrayList<Operator>() {{
            add(new EqualOperator("unitId", "myUnit"));
            add(new GreaterThanOperator("customClientData.weight", 12));
        }});

        // When
        final Page<Device> result = deviceRepository.findByUserId(userId, searchFilter, pageable);

        // Then
        assertThat(result).isEmpty();
    }

    /*
    @Test
    public void findByUserId_WhenValidQueryGiven_shouldReturnFilteredDevicesThatBelongToUser() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId1 = "TheCoolestUnitEver";
        final String unitId2 = UUID.randomUUID().toString();
        final String unitId3 = UUID.randomUUID().toString();
        final String unitId4 = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 50);
        final Operator searchFilter = new OrOperator(new ArrayList<Operator>() {{
            add(new EqualOperator("unitId", unitId1));
            add(new GreaterThanOperator("customClientData.weight", 12));
        }});
        final HashMap<String, Object> matchingData = new HashMap<>();
        final HashMap<String, Object> notMatchingData = new HashMap<>();
        matchingData.put("weight", 13);
        notMatchingData.put("weight", 5);

        final List<Device> userDevices = new ArrayList<>();
        final List<Device> expectedResult = new ArrayList<>();
        final DeviceEvent deviceEvent1 = DeviceEvent.builder().unitId(unitId1).userId(userId).additionalProperties(notMatchingData).build();
        final DeviceEvent deviceEvent2 = DeviceEvent.builder().unitId(unitId2).userId(userId).additionalProperties(matchingData).build();
        final DeviceEvent deviceEvent3 = DeviceEvent.builder().unitId(unitId3).userId(userId).additionalProperties(matchingData).build();
        final DeviceEvent deviceEvent4 = DeviceEvent.builder().unitId(unitId4).userId(userId).additionalProperties(notMatchingData).build();
        final Device device1 = Device.builder().userId(userId).lastEvent(deviceEvent1).unitId(unitId1).build();
        final Device device2 = Device.builder().userId(userId).lastEvent(deviceEvent2).unitId(unitId2).build();
        final Device device3 = Device.builder().userId(userId).lastEvent(deviceEvent3).unitId(unitId3).build();
        final Device device4 = Device.builder().userId(userId).lastEvent(deviceEvent4).unitId(unitId4).build();
        userDevices.add(device1);
        userDevices.add(device2);
        userDevices.add(device3);
        userDevices.add(device4);
        expectedResult.add(device1);
        expectedResult.add(device2);
        expectedResult.add(device3);

        final List<Device> otherDevices = getDevicesForUser("anotherUser", 10);
        final List<Device> allDevices = new ArrayList<Device>() {{
            addAll(userDevices);
            addAll(otherDevices);
        }};
        allDevices.parallelStream().forEach(device -> deviceRepository.save(device));

        // When
        final Page<Device> result = deviceRepository.findByUserId(userId, searchFilter, pageable);

        // Then
        assertThat(result).containsOnlyElementsOf(expectedResult);
    }
    */

    @Test
    public void getDeviceCountPerUserId_shouldReturnDeviceCountPerUserId() {
        // Given
        final String userId1 = UUID.randomUUID().toString();
        final String userId2 = UUID.randomUUID().toString();
        deviceRepository.insert(getDevice(userId1));
        deviceRepository.insert(getDevice(userId1));
        deviceRepository.insert(getDevice(userId2));
        deviceRepository.insert(getDevice(userId2));
        deviceRepository.insert(getDevice(userId2));
        final DataSet expected = DataSet.builder()
                .value(userId1, BigDecimal.valueOf(2))
                .value(userId2, BigDecimal.valueOf(3))
                .total(BigDecimal.valueOf(5))
                .build();

        // When
        final DataSet result = deviceRepository.getDeviceCountPerUserId();

        // Then
        assertThat(result.getValues()).containsAllEntriesOf(expected.getValues());
        assertThat(result.getTotal()).isEqualTo(expected.getTotal());
    }

    private Device getDevice(String userId) {
        return DeviceUtils.getDevice().toBuilder().userId(userId).build();
    }

    private List<Device> getDevicesForUser(String userId, int numberOfDevices) {
        final ArrayList<Device> devices = new ArrayList<>();
        while (numberOfDevices != 0) {
            --numberOfDevices;
            devices.add(getDevice(userId));
        }
        return devices;
    }
}
