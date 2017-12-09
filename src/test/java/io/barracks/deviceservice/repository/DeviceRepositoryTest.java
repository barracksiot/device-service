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

import io.barracks.commons.test.MongoRepositoryTest;
import io.barracks.deviceservice.model.DataSet;
import io.barracks.deviceservice.model.Device;
import io.barracks.deviceservice.model.DeviceConfiguration;
import io.barracks.deviceservice.model.DeviceEvent;
import io.barracks.deviceservice.model.operator.Operator;
import io.barracks.deviceservice.model.operator.comparison.EqualOperator;
import io.barracks.deviceservice.model.operator.comparison.GreaterThanOperator;
import io.barracks.deviceservice.model.operator.logical.AndOperator;
import io.barracks.deviceservice.model.operator.logical.OrOperator;
import io.barracks.deviceservice.utils.DeviceUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeviceRepositoryTest extends MongoRepositoryTest {
    private DeviceRepositoryImpl deviceRepository;
    private MongoTemplate mongoTemplate;

    public DeviceRepositoryTest() {
        super("units");
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mongoTemplate = new MongoTemplate(getMongo(), getDatabaseName());
        deviceRepository = spy(new DeviceRepositoryImpl(mongoTemplate));
    }

    @Test
    public void updateDocument_whenDocumentDoesNotExist_shouldCreateDocumentWithSubDocumentAndReturnUpdatedUnit() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceConfiguration configuration = DeviceConfiguration.builder().unitId(unitId).userId(userId).build();
        final Device expected = Device.builder().userId(userId).unitId(unitId).configuration(configuration).build();

        // When
        final Device result = deviceRepository.updateDocument(userId, unitId, "configuration", configuration);

        // Then
        assertThat(result.toBuilder().id(null).build()).isEqualTo(expected);
    }

    @Test
    public void updateDocument_whenDocumentExists_shouldUpdateSpecifiedDocumentAndReturnUpdatedUnit() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceConfiguration configuration = DeviceConfiguration.builder().unitId(unitId).userId(userId).build();
        final DeviceEvent event = DeviceEvent.builder().userId(userId).unitId(unitId).build();
        final Device existing = Device.builder().userId(userId).unitId(unitId).configuration(configuration).build();
        mongoTemplate.save(existing);
        final Device expected = Device.builder().userId(userId).unitId(unitId).configuration(configuration).lastEvent(event).build();

        // When
        final Device result = deviceRepository.updateDocument(userId, unitId, "event", event);

        // Then
        assertThat(result.toBuilder().id(null).build()).isEqualTo(expected);
    }

    @Test
    public void updateDocument_whenDocumentExistsWithSameProperty_shouldOverridePropertyAndReturnUpdatedUnit() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        final DeviceConfiguration configuration = DeviceConfiguration.builder().unitId(unitId).userId(userId).build();
        final DeviceEvent event = DeviceEvent.builder().userId(userId).unitId(unitId).build();
        final Device existing = Device.builder().userId(userId).unitId(unitId).configuration(configuration).lastEvent(event).build();
        mongoTemplate.save(existing);
        final DeviceEvent newEvent = DeviceEvent.builder().userId(userId).unitId(unitId).segmentId(segmentId).build();
        final Device expected = Device.builder().userId(userId).unitId(unitId).configuration(configuration).lastEvent(newEvent).build();

        // When
        final Device result = deviceRepository.updateDocument(userId, unitId, "event", newEvent);

        // Then
        assertThat(result.toBuilder().id(null).build()).isEqualTo(expected);
    }

    @Test
    public void updateConfiguration_shouldUpdateConfigurationSubDocument() {
        // Given
        final String configurationKey = "configuration";
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceConfiguration configuration = DeviceConfiguration.builder().userId(userId).unitId(unitId).build();
        final Device expected = Device.builder().userId(userId).unitId(unitId).configuration(configuration).build();
        doReturn(expected).when(deviceRepository).updateDocument(userId, unitId, configurationKey, configuration);

        // When
        deviceRepository.updateConfiguration(userId, unitId, configuration);

        // Then
        verify(deviceRepository).updateDocument(userId, unitId, configurationKey, configuration);
    }

    @Test
    public void updateEvent_shouldUpdateEventSubDocument() {
        // Given
        final String eventKey = "event";
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceEvent event = DeviceEvent.builder().userId(userId).unitId(unitId).build();
        final Device expected = Device.builder().userId(userId).unitId(unitId).lastEvent(event).build();
        doReturn(expected).when(deviceRepository).updateDocument(userId, unitId, eventKey, event);

        // When
        deviceRepository.updateDeviceEvent(userId, unitId, event);

        // Then
        verify(deviceRepository).updateDocument(userId, unitId, eventKey, event);
    }

    @Test
    public void updateFirstSeen_shouldUpdateFirstSeenSubDocument() {
        // Given
        final String firstSeenKey = "firstSeen";
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Date firstSeen = new Date(1234567890L);
        final DeviceEvent event = DeviceEvent.builder().userId(userId).unitId(unitId).build();
        final Device expected = Device.builder().firstSeen(firstSeen).userId(userId).unitId(unitId).lastEvent(event).build();
        doReturn(expected).when(deviceRepository).updateDocument(userId, unitId, firstSeenKey, firstSeen);

        // When
        deviceRepository.updateFirstSeen(userId, unitId, firstSeen);

        // Then
        verify(deviceRepository).updateDocument(userId, unitId, firstSeenKey, firstSeen);
    }

    @Test
    public void findByUserId_WhenNoQueryGivenAndNoDevice_shouldReturnEmptyPage() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);

        // When
        final Page<Device> result = deviceRepository.findByUserId(userId, Optional.empty(), pageable);

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
        allDevices.parallelStream().forEach(device -> mongoTemplate.save(device));

        // When
        final Page<Device> result = deviceRepository.findByUserId(userId, Optional.empty(), pageable);

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
        final Page<Device> result = deviceRepository.findByUserId(userId, Optional.of(searchFilter), pageable);

        // Then
        assertThat(result).isEmpty();
    }

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
        allDevices.parallelStream().forEach(device -> mongoTemplate.save(device));

        // When
        final Page<Device> result = deviceRepository.findByUserId(userId, Optional.of(searchFilter), pageable);

        // Then
        assertThat(result).containsOnlyElementsOf(expectedResult);
    }

    @Test
    public void findBySegmentId_shouldReturnAllDevicesWithSegmentId() {
        // Given
        final String segmentId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Device expected = Device.builder().lastEvent(DeviceEvent.builder().segmentId(segmentId).build()).build();
        mongoTemplate.save(expected);

        // When
        final Page<Device> result = deviceRepository.findBySegmentId(segmentId, pageable);

        // Then
        assertThat(result).containsExactly(expected);
    }

    @Test
    public void findBySegmentId_shouldIgnoreAllDevicesWithoutSegmentId() {
        // Given
        final String segmentId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Device expected = Device.builder().lastEvent(DeviceEvent.builder().segmentId(UUID.randomUUID().toString()).build()).build();
        mongoTemplate.save(expected);

        // When
        final Page<Device> result = deviceRepository.findBySegmentId(segmentId, pageable);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void findDevicesNotIn_shouldReturnAllDevicesFromUserWithoutProvidedIds() {
        // Given
        final String segmentId = UUID.randomUUID().toString();
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Device expected = Device.builder()
                .userId(userId)
                .lastEvent(DeviceEvent.builder().segmentId(segmentId).build())
                .build();
        final List<String> segmentIds = Collections.singletonList(UUID.randomUUID().toString());
        mongoTemplate.save(expected);

        // When
        final Page<Device> result = deviceRepository.findDevicesNotIn(userId, segmentIds, pageable);

        // Then
        assertThat(result).containsExactly(expected);
    }

    @Test
    public void findDevicesNotIn_whenNoProvidedId_shouldReturnAllDevicesFromUser() {
        // Given
        final String segmentId = UUID.randomUUID().toString();
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Device[] expected = new Device[]{
                Device.builder()
                        .unitId(UUID.randomUUID().toString())
                        .userId(userId)
                        .lastEvent(DeviceEvent.builder().segmentId(segmentId).build())
                        .build(),
                Device.builder().userId(userId)
                        .unitId(UUID.randomUUID().toString())
                        .userId(userId)
                        .lastEvent(DeviceEvent.builder().build())
                        .build()
        };
        final List<String> segmentIds = Collections.emptyList();
        mongoTemplate.save(expected[0]);
        mongoTemplate.save(expected[1]);

        // When
        final Page<Device> result = deviceRepository.findDevicesNotIn(userId, segmentIds, pageable);

        // Then
        assertThat(result).containsExactlyInAnyOrder(expected);
    }

    @Test
    public void findDevicesNotIn_shouldReturnDevicesWithoutSegmentId() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Device expected = Device.builder()
                .userId(userId)
                .lastEvent(DeviceEvent.builder().build())
                .build();
        final List<String> segmentIds = Collections.emptyList();
        mongoTemplate.save(expected);

        // When
        final Page<Device> result = deviceRepository.findDevicesNotIn(userId, segmentIds, pageable);

        // Then
        assertThat(result).containsExactly(expected);
    }

    @Test
    public void findDevicesNotIn_shouldIgnoreDevicesWithoutEvent() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Device expected = Device.builder()
                .userId(userId)
                .build();
        final List<String> segmentIds = Collections.emptyList();
        mongoTemplate.save(expected);

        // When
        final Page<Device> result = deviceRepository.findDevicesNotIn(userId, segmentIds, pageable);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void findBySegmentIdAndVersionId_shouldReturnAllDevicesWithSegmentIdAndVersionId() {
        // Given
        final String segmentId = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Device expected = Device.builder().lastEvent(DeviceEvent.builder().segmentId(segmentId).versionId(versionId).build()).build();
        mongoTemplate.save(expected);

        // When
        final Page<Device> result = deviceRepository.findBySegmentId(segmentId, pageable);

        // Then
        assertThat(result).containsExactly(expected);
    }

    @Test
    public void findBySegmentIdAndVersionId_shouldIgnoreAllDevicesWithoutSegmentId() {
        // Given
        final String segmentId = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Device expected = Device.builder().lastEvent(DeviceEvent.builder().segmentId(UUID.randomUUID().toString()).versionId(versionId).build()).build();
        mongoTemplate.save(expected);

        // When
        final Page<Device> result = deviceRepository.findBySegmentIdAndVersionId(segmentId, versionId, pageable);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void findBySegmentIdAndVersionId_shouldIgnoreAllDevicesWithoutVersionId() {
        // Given
        final String segmentId = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Device expected = Device.builder().lastEvent(DeviceEvent.builder().segmentId(segmentId).versionId(UUID.randomUUID().toString()).build()).build();
        mongoTemplate.save(expected);

        // When
        final Page<Device> result = deviceRepository.findBySegmentIdAndVersionId(segmentId, versionId, pageable);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void findForUserIdAndVersionIdAndNotSegmentIds_shouldReturnAllDevicesFromUserAndVersionWithoutProvidedIds() {
        // Given
        final String segmentId = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final List<Device> expected = Arrays.asList(
                getDeviceForUserAndVersionAndSegment(userId, versionId, segmentId),
                getDeviceForUserAndVersionAndSegment(userId, versionId, segmentId)
        );
        final List<String> segmentIds = Collections.singletonList(UUID.randomUUID().toString());
        mongoTemplate.insertAll(expected);

        // When
        final Page<Device> result = deviceRepository.findForUserIdAndVersionIdAndNotSegmentIds(userId, versionId, segmentIds, pageable);

        // Then
        assertThat(result).containsOnlyElementsOf(expected);
    }

    @Test
    public void findForUserIdAndVersionIdAndNotSegmentIds_shouldIgnoreDevicesWithDifferentVersionId() {
        // Given
        final String segmentId = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final List<Device> expected = Arrays.asList(
                getDeviceForUserAndVersionAndSegment(userId, UUID.randomUUID().toString(), segmentId),
                getDeviceForUserAndVersionAndSegment(userId, UUID.randomUUID().toString(), segmentId)
        );
        final List<String> segmentIds = Collections.singletonList(UUID.randomUUID().toString());
        mongoTemplate.insertAll(expected);

        // When
        final Page<Device> result = deviceRepository.findForUserIdAndVersionIdAndNotSegmentIds(userId, versionId, segmentIds, pageable);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void findForUserIdAndVersionIdAndNotSegmentIds_whenNoProvidedId_shouldReturnAllDevicesFromUserWithVersionId() {
        // Given
        final String segmentId = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final List<Device> expected = Arrays.asList(
                getDeviceForUserAndVersionAndSegment(userId, versionId, segmentId),
                getDeviceForUserAndVersionAndSegment(userId, versionId, segmentId)
        );
        final List<String> segmentIds = Collections.emptyList();
        mongoTemplate.insertAll(expected);

        // When
        final Page<Device> result = deviceRepository.findForUserIdAndVersionIdAndNotSegmentIds(userId, versionId, segmentIds, pageable);

        // Then
        assertThat(result).containsOnlyElementsOf(expected);
    }

    @Test
    public void findForUserIdAndVersionIdAndNotSegmentIds_shouldReturnDevicesWithVersionIdWithoutSegmentId() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final List<Device> expected = Arrays.asList(
                getDeviceForUserAndVersionAndSegment(userId, versionId, null),
                getDeviceForUserAndVersionAndSegment(userId, versionId, null)
        );
        final List<String> segmentIds = Collections.singletonList(UUID.randomUUID().toString());
        mongoTemplate.insertAll(expected);

        // When
        final Page<Device> result = deviceRepository.findForUserIdAndVersionIdAndNotSegmentIds(userId, versionId, segmentIds, pageable);

        // Then
        assertThat(result).containsOnlyElementsOf(expected);
    }

    @Test
    public void findForUserIdAndVersionIdAndNotSegmentIds_shouldIgnoreDevicesWithoutEvent() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Device expected = Device.builder()
                .userId(userId)
                .build();
        final List<String> segmentIds = Collections.singletonList(UUID.randomUUID().toString());
        mongoTemplate.save(expected);

        // When
        final Page<Device> result = deviceRepository.findForUserIdAndVersionIdAndNotSegmentIds(userId, versionId, segmentIds, pageable);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void getDeviceCountPerUserId_shouldReturnDeviceCountPerUserId() {
        // Given
        final String userId1 = UUID.randomUUID().toString();
        final String userId2 = UUID.randomUUID().toString();
        mongoTemplate.insert(getDeviceForUser(userId1));
        mongoTemplate.insert(getDeviceForUser(userId1));
        mongoTemplate.insert(getDeviceForUser(userId2));
        mongoTemplate.insert(getDeviceForUser(userId2));
        mongoTemplate.insert(getDeviceForUser(userId2));
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

    private Device getDeviceForUser(String userId) {
        return getDeviceForUserAndVersion(userId, UUID.randomUUID().toString());
    }

    private Device getDeviceForUserAndVersion(String userId, String version) {
        return getDeviceForUserAndVersionAndSegment(userId, version, UUID.randomUUID().toString());
    }

    private Device getDeviceForUserAndVersionAndSegment(String userId, String version, String segment) {
        final Device device = DeviceUtils.getDevice();
        final DeviceEvent event = device.getLastEvent().toBuilder()
                .versionId(version)
                .segmentId(segment)
                .build();
        return device.toBuilder()
                .userId(userId)
                .lastEvent(event)
                .build();
    }

    private List<Device> getDevicesForUser(String userId, int numberOfDevices) {
        final ArrayList<Device> devices = new ArrayList<>();
        while (numberOfDevices != 0) {
            --numberOfDevices;
            devices.add(getDeviceForUser(userId));
        }
        return devices;
    }
}
