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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.Optional;

@Service
@Slf4j
public class DeviceManager {

    private final DeviceEventRepository deviceEventRepository;
    private final DeviceRepository deviceRepository;

    private final RabbitMQClient rabbitMQClient;

    @Autowired
    public DeviceManager(
            DeviceEventRepository deviceEventRepository,
            DeviceRepository deviceRepository,
            RabbitMQClient rabbitMQClient) {
        this.deviceEventRepository = deviceEventRepository;
        this.deviceRepository = deviceRepository;
        this.rabbitMQClient = rabbitMQClient;
    }

    public DeviceEvent saveDeviceEvent(DeviceEvent receivedEvent) {
        final String userId = receivedEvent.getUserId();
        final String unitId = receivedEvent.getUnitId();

        final Optional<Device> device = deviceRepository.findByUserIdAndUnitId(userId, unitId);
        boolean changed = deviceChanged(device, receivedEvent);
        final DeviceEvent toSave = receivedEvent.toBuilder().userId(userId).unitId(unitId).changed(changed).build();
        final DeviceEvent saved = deviceEventRepository.save(toSave);

        if (!device.isPresent() || !device.get().getLastEvent().isPresent()) {
            rabbitMQClient.postDeviceChange(null, saved, EventType.DEVICE_DATA_CHANGE);
            rabbitMQClient.postDeviceChange(null, saved, EventType.DEVICE_PACKAGE_CHANGE);
        } else {
            final DeviceRequest oldDeviceRequest = device.get().getLastEvent().get().getRequest();
            if (deviceDataChanged(device, receivedEvent)) {
                rabbitMQClient.postDeviceChange(oldDeviceRequest, saved, EventType.DEVICE_DATA_CHANGE);
            }
            if (devicePackagesChanged(device, receivedEvent)) {
                rabbitMQClient.postDeviceChange(oldDeviceRequest, saved, EventType.DEVICE_PACKAGE_CHANGE);
            }
        }

        if (deviceRepository.updateLastEvent(userId, unitId, saved)) {
            rabbitMQClient.postEnrollmentEvent(saved);
        }


        return saved;
    }

    private boolean deviceChanged(Optional<Device> device, DeviceEvent receivedEvent) {
        return device.flatMap(Device::getLastEvent)
                .map(lastEvent -> {
                    final DeviceEvent toCompare = getDeviceEventToCompare(receivedEvent, lastEvent);
                    return !lastEvent.equals(toCompare);
                })
                .orElse(true);
    }

    private boolean deviceDataChanged(Optional<Device> device, DeviceEvent receivedEvent) {
        return device.flatMap(Device::getLastEvent)
                .map(lastEvent -> {
                    final DeviceEvent toCompare = getDeviceEventToCompare(receivedEvent, lastEvent);
                    return !(lastEvent.getRequest().getCustomClientData()).equals(toCompare.getRequest().getCustomClientData());
                })
                .orElse(true);
    }

    private boolean devicePackagesChanged(Optional<Device> device, DeviceEvent receivedEvent) {
        return device.flatMap(Device::getLastEvent)
                .map(lastEvent -> {
                    final DeviceEvent toCompare = getDeviceEventToCompare(receivedEvent, lastEvent);
                    return !(lastEvent.getRequest().getPackages()).equals(toCompare.getRequest().getPackages());
                })
                .orElse(true);
    }

    private DeviceEvent getDeviceEventToCompare(DeviceEvent receivedEvent, DeviceEvent lastEvent) {
        return lastEvent.toBuilder()
                .request(receivedEvent.getRequest())
                .response(receivedEvent.getResponse())
                .build();
    }

    public Page<Device> getDevices(String userId, @Nullable Operator query, Pageable pageable) {
        return deviceRepository.findByUserId(userId, query, pageable);
    }

    public Page<DeviceEvent> getDeviceEvents(String userId, String unitId, boolean onlyChanged, Pageable pageable) {
        return deviceEventRepository.findByUserIdAndUnitId(userId, unitId, onlyChanged, pageable);
    }

    public Device getDevice(String userId, String unitId) {
        return deviceRepository.findByUserIdAndUnitId(userId, unitId).orElseThrow(DeviceNotFoundException::new);
    }
}
