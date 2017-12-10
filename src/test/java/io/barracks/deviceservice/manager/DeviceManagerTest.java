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

package io.barracks.deviceservice.manager;

import io.barracks.deviceservice.client.RabbitMQClient;
import io.barracks.deviceservice.manager.exception.DeviceNotFoundException;
import io.barracks.deviceservice.model.Device;
import io.barracks.deviceservice.model.DeviceEvent;
import io.barracks.deviceservice.model.DeviceRequest;
import io.barracks.deviceservice.model.EventType;
import io.barracks.deviceservice.model.operator.Operator;
import io.barracks.deviceservice.repository.DeviceEventRepository;
import io.barracks.deviceservice.repository.DeviceRepository;
import io.barracks.deviceservice.utils.PackageUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static io.barracks.deviceservice.utils.DeviceEventUtils.getDeviceEvent;
import static io.barracks.deviceservice.utils.DeviceUtils.getDevice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeviceManagerTest {

    @Mock
    private DeviceEventRepository deviceEventRepository;

    @Mock
    private RabbitMQClient rabbitMQClient;

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private DeviceManager deviceManager;

    @Test
    public void saveDeviceEvent_whenDeviceWasNotRegisteredEarlier_shouldSaveEventWithChangedAndCallRabbitClient() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceEvent event = getDeviceEvent().toBuilder().userId(userId).unitId(unitId).changed(false).build();
        final DeviceEvent expectedToSave = event.toBuilder().userId(userId).unitId(unitId).changed(true).build();
        final DeviceEvent expected = getDeviceEvent();

        doReturn(Optional.empty()).when(deviceRepository).findByUserIdAndUnitId(userId, unitId);
        doReturn(expected).when(deviceEventRepository).save(expectedToSave);
        doReturn(true).when(deviceRepository).updateLastEvent(userId, unitId, expected);
        doNothing().when(rabbitMQClient).postEnrollmentEvent(expected);

        // When
        final DeviceEvent result = deviceManager.saveDeviceEvent(event);

        // Then
        verify(deviceRepository).findByUserIdAndUnitId(userId, unitId);
        verify(deviceEventRepository).save(expectedToSave);
        verify(deviceRepository).updateLastEvent(userId, unitId, expected);
        verify(rabbitMQClient).postEnrollmentEvent(expected);
        verify(rabbitMQClient).postDeviceChange(null, expected, EventType.DEVICE_DATA_CHANGE);
        verify(rabbitMQClient).postDeviceChange(null, expected, EventType.DEVICE_PACKAGE_CHANGE);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void saveDeviceEvent_whenDeviceWasRegisteredEarlierWithoutEvent_shouldSaveEventWithChanged() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Device device = getDevice().toBuilder().lastEvent(null).build();
        final DeviceEvent event = getDeviceEvent().toBuilder().userId(userId).unitId(unitId).changed(false).build();
        final DeviceEvent expectedToSave = event.toBuilder().userId(userId).unitId(unitId).changed(true).build();
        final DeviceEvent expected = getDeviceEvent();

        doReturn(Optional.of(device)).when(deviceRepository).findByUserIdAndUnitId(userId, unitId);
        doReturn(expected).when(deviceEventRepository).save(expectedToSave);
        doReturn(false).when(deviceRepository).updateLastEvent(userId, unitId, expected);

        // When
        final DeviceEvent result = deviceManager.saveDeviceEvent(event);

        // Then
        verify(deviceRepository).findByUserIdAndUnitId(userId, unitId);
        verify(deviceEventRepository).save(expectedToSave);
        verify(deviceRepository).updateLastEvent(userId, unitId, expected);
        verify(rabbitMQClient, never()).postEnrollmentEvent(expected);
        verify(rabbitMQClient).postDeviceChange(null, expected, EventType.DEVICE_DATA_CHANGE);
        verify(rabbitMQClient).postDeviceChange(null, expected, EventType.DEVICE_PACKAGE_CHANGE);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void saveDeviceEvent_whenDeviceWasRegisteredEarlierWithDifferentEvent_shouldSaveEventWithChanged() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Device device = getDevice();
        final DeviceEvent event = getDeviceEvent().toBuilder().userId(userId).unitId(unitId).changed(false).build();
        final DeviceEvent expectedToSave = event.toBuilder().userId(userId).unitId(unitId).changed(true).build();
        final DeviceEvent expected = getDeviceEvent();

        doReturn(Optional.of(device)).when(deviceRepository).findByUserIdAndUnitId(userId, unitId);
        doReturn(expected).when(deviceEventRepository).save(expectedToSave);
        doReturn(false).when(deviceRepository).updateLastEvent(userId, unitId, expected);

        // When
        final DeviceEvent result = deviceManager.saveDeviceEvent(event);

        // Then
        verify(deviceRepository).findByUserIdAndUnitId(userId, unitId);
        verify(deviceEventRepository).save(expectedToSave);
        verify(deviceRepository).updateLastEvent(userId, unitId, expected);
        verify(rabbitMQClient, never()).postEnrollmentEvent(expected);
        verify(rabbitMQClient).postDeviceChange(device.getLastEvent().get().getRequest(), expected, EventType.DEVICE_DATA_CHANGE);
        verify(rabbitMQClient).postDeviceChange(device.getLastEvent().get().getRequest(), expected, EventType.DEVICE_PACKAGE_CHANGE);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void saveDeviceEvent_whenDeviceWasRegisteredEarlierWithDifferentPackages_shouldSaveEventWithChanged() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceEvent event = getDeviceEvent().toBuilder().userId(userId).unitId(unitId).changed(false).build();
        final DeviceEvent lastEvent = event.toBuilder().request(
                event.getRequest().toBuilder()
                        .addPackage(PackageUtils.getPackage())
                        .addPackage(PackageUtils.getPackage())
                        .build()
        ).build();
        final Device device = getDevice().toBuilder().lastEvent(lastEvent).build();
        final DeviceEvent expectedToSave = event.toBuilder().userId(userId).unitId(unitId).changed(true).build();
        final DeviceEvent expected = getDeviceEvent();

        doReturn(Optional.of(device)).when(deviceRepository).findByUserIdAndUnitId(userId, unitId);
        doReturn(expected).when(deviceEventRepository).save(expectedToSave);
        doReturn(false).when(deviceRepository).updateLastEvent(userId, unitId, expected);

        // When
        final DeviceEvent result = deviceManager.saveDeviceEvent(event);

        // Then
        verify(deviceRepository).findByUserIdAndUnitId(userId, unitId);
        verify(deviceEventRepository).save(expectedToSave);
        verify(deviceRepository).updateLastEvent(userId, unitId, expected);
        verify(rabbitMQClient, never()).postEnrollmentEvent(expected);
        verify(rabbitMQClient, never()).postDeviceChange(any(DeviceRequest.class), any(DeviceEvent.class), eq(EventType.DEVICE_DATA_CHANGE));
        verify(rabbitMQClient).postDeviceChange(device.getLastEvent().get().getRequest(), expected, EventType.DEVICE_PACKAGE_CHANGE);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void saveDeviceEvent_whenDeviceWasRegisteredEarlierWithDifferentCustomClientData_shouldSaveEventWithChanged() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceEvent event = getDeviceEvent().toBuilder().userId(userId).unitId(unitId).changed(false).build();
        final DeviceEvent lastEvent = event.toBuilder().request(
                event.getRequest().toBuilder()
                        .addCustomClientData("clientDataKey", "clientDataValue")
                        .addCustomClientData("data", "value")
                        .build()
        ).build();
        final Device device = getDevice().toBuilder().lastEvent(lastEvent).build();
        final DeviceEvent expectedToSave = event.toBuilder().userId(userId).unitId(unitId).changed(true).build();
        final DeviceEvent expected = getDeviceEvent();

        doReturn(Optional.of(device)).when(deviceRepository).findByUserIdAndUnitId(userId, unitId);
        doReturn(expected).when(deviceEventRepository).save(expectedToSave);
        doReturn(false).when(deviceRepository).updateLastEvent(userId, unitId, expected);

        // When
        final DeviceEvent result = deviceManager.saveDeviceEvent(event);

        // Then
        verify(deviceRepository).findByUserIdAndUnitId(userId, unitId);
        verify(deviceEventRepository).save(expectedToSave);
        verify(deviceRepository).updateLastEvent(userId, unitId, expected);
        verify(rabbitMQClient, never()).postEnrollmentEvent(expected);
        verify(rabbitMQClient).postDeviceChange(device.getLastEvent().get().getRequest(), expected, EventType.DEVICE_DATA_CHANGE);
        verify(rabbitMQClient, never()).postDeviceChange(any(DeviceRequest.class), any(DeviceEvent.class), eq(EventType.DEVICE_PACKAGE_CHANGE));
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void saveDeviceEvent_whenDeviceWasRegisteredEarlierWithSameEvent_shouldSaveEventWithUnchanged() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceEvent event = getDeviceEvent().toBuilder().userId(userId).unitId(unitId).changed(true).build();
        final Device device = getDevice().toBuilder().lastEvent(event).build();
        final DeviceEvent expectedToSave = event.toBuilder().userId(userId).unitId(unitId).changed(false).build();
        final DeviceEvent expected = getDeviceEvent();

        doReturn(Optional.of(device)).when(deviceRepository).findByUserIdAndUnitId(userId, unitId);
        doReturn(expected).when(deviceEventRepository).save(expectedToSave);
        doReturn(false).when(deviceRepository).updateLastEvent(userId, unitId, expected);

        // When
        final DeviceEvent result = deviceManager.saveDeviceEvent(event);

        // Then
        verify(deviceRepository).findByUserIdAndUnitId(userId, unitId);
        verify(deviceEventRepository).save(expectedToSave);
        verify(deviceRepository).updateLastEvent(userId, unitId, expected);
        verify(rabbitMQClient, never()).postEnrollmentEvent(expected);
        verify(rabbitMQClient, never()).postDeviceChange(any(DeviceRequest.class), any(DeviceEvent.class), eq(EventType.DEVICE_DATA_CHANGE));
        verify(rabbitMQClient, never()).postDeviceChange(any(DeviceRequest.class), any(DeviceEvent.class), eq(EventType.DEVICE_PACKAGE_CHANGE));
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getDevices_shouldForwardCallToTheRepository() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Operator operator = mock(Operator.class);
        final Page<Device> expected = new PageImpl<>(Collections.emptyList());
        doReturn(expected).when(deviceRepository).findByUserId(userId, operator, pageable);

        // When
        final Page<Device> result = deviceManager.getDevices(userId, operator, pageable);

        // Then
        verify(deviceRepository).findByUserId(userId, operator, pageable);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getDeviceEvents_shouldForwardCallToTheRepository() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Page<DeviceEvent> page = new PageImpl<>(Arrays.asList(getDeviceEvent(), getDeviceEvent()));
        final boolean changed = true;
        doReturn(page).when(deviceEventRepository).findByUserIdAndUnitId(userId, unitId, changed, pageable);

        // When
        final Page<DeviceEvent> result = deviceManager.getDeviceEvents(userId, unitId, changed, pageable);

        // Then
        verify(deviceEventRepository).findByUserIdAndUnitId(userId, unitId, changed, pageable);
        assertThat(result).isEqualTo(page);
    }

    @Test
    public void getDevice_whenDeviceExists_shouldReturnUnit() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Device expected = getDevice();
        doReturn(Optional.of(expected)).when(deviceRepository).findByUserIdAndUnitId(userId, unitId);

        // When
        Device result = deviceManager.getDevice(userId, unitId);

        // Then
        verify(deviceRepository).findByUserIdAndUnitId(userId, unitId);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getDevice_whenUnitDoesNotExist_shouldThrowException() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        doReturn(Optional.empty()).when(deviceRepository).findByUserIdAndUnitId(userId, unitId);

        // Then when
        assertThatExceptionOfType(DeviceNotFoundException.class)
                .isThrownBy(() -> deviceManager.getDevice(userId, unitId));
        verify(deviceRepository).findByUserIdAndUnitId(userId, unitId);
    }

}
