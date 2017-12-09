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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.deviceservice.manager.exception.DeviceNotFoundException;
import io.barracks.deviceservice.model.*;
import io.barracks.deviceservice.model.operator.Operator;
import io.barracks.deviceservice.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class DeviceManager {

    private final DeviceEventRepository deviceEventRepository;

    private final DeviceConfigurationRepository deviceConfigurationRepository;

    private final DeviceRepository deviceRepository;


    private final SegmentRepository segmentRepository;

    private final SegmentOrderRepository segmentOrderRepository;

    @Autowired
    public DeviceManager(
            DeviceEventRepository deviceEventRepository,
            DeviceConfigurationRepository deviceConfigurationRepository,
            DeviceRepository deviceRepository,
            SegmentRepository segmentRepository,
            SegmentOrderRepository segmentOrderRepository) {
        this.deviceEventRepository = deviceEventRepository;
        this.deviceConfigurationRepository = deviceConfigurationRepository;
        this.deviceRepository = deviceRepository;
        this.segmentRepository = segmentRepository;
        this.segmentOrderRepository = segmentOrderRepository;
    }

    public DeviceEvent saveDeviceEvent(DeviceEvent receivedEvent) {
        // Init and retrieve basic information
        final Date creationDate = createReceptionDate();
        Device device = getOrCreateDevice(receivedEvent.getUserId(), receivedEvent.getUnitId());
        if (device.hasPinged()) {
            device = deviceRepository.updateFirstSeen(receivedEvent.getUserId(), receivedEvent.getUnitId(), creationDate);
        }

        // Prepare base results from what we already have
        final DeviceEvent.DeviceEventBuilder processedEventBuilder = DeviceEvent.builder()
                .userId(receivedEvent.getUserId())
                .unitId(receivedEvent.getUnitId())
                .versionId(receivedEvent.getVersionId())
                .deviceIP(receivedEvent.getDeviceIP())
                .receptionDate(creationDate)
                .additionalProperties(receivedEvent.getAdditionalProperties());

        // Find operator Id and check for changes
        final String segmentId = getExclusiveSegmentId(device, processedEventBuilder.build()).orElse(null);
        final DeviceEvent latestEvent = device.getLastEvent();
        final boolean changed = hasChanged(receivedEvent, segmentId, latestEvent);

        // Complete final event computation and save
        final DeviceEvent toSave = processedEventBuilder
                .segmentId(segmentId)
                .changed(changed)
                .build();
        final DeviceEvent saved = deviceEventRepository.save(toSave);
        deviceRepository.updateDeviceEvent(receivedEvent.getUserId(), receivedEvent.getUnitId(), saved);
        return saved;
    }

    Optional<String> getExclusiveSegmentId(Device device, DeviceEvent event) {
        final Device nextDevice = device.toBuilder()
                .lastEvent(event)
                .build();
        final JsonNode jsonDevice = new ObjectMapper().valueToTree(nextDevice);
        final SegmentOrder order = segmentOrderRepository.findByUserId(device.getUserId());
        final List<Segment> segments = segmentRepository.getSegmentsInIds(device.getUserId(), order.getSegmentIds());
        Optional<Segment> electedSegment = segments.stream().filter(segment -> segment.getQuery().matches(jsonDevice)).findFirst();
        if (electedSegment.isPresent()) {
            return Optional.of(electedSegment.get().getId());
        }
        return Optional.empty();
    }

    boolean hasChanged(DeviceEvent source, String segmentId, @Nullable DeviceEvent latest) {
        if (latest != null) {
            final DeviceEvent toCompare = DeviceEvent.create(
                    latest.getId(),
                    source.getUnitId(),
                    source.getUserId(),
                    source.getVersionId(),
                    latest.getReceptionDate(),
                    source.getAdditionalProperties(),
                    latest.isChanged(),
                    segmentId,
                    source.getDeviceIP()
            );
            return !toCompare.equals(latest);
        }
        return true;
    }

    Date createReceptionDate() {
        return new Date();
    }

    public DeviceConfiguration saveDeviceConfiguration(String userId, String unitId, DeviceConfiguration configuration) {
        final DeviceConfiguration toSave = DeviceConfiguration.builder()
                .userId(userId)
                .unitId(unitId)
                .creationDate(createReceptionDate())
                .build();
        DeviceConfiguration saved = deviceConfigurationRepository.save(toSave);
        deviceRepository.updateConfiguration(userId, unitId, saved);
        return saved;
    }

    Device getOrCreateDevice(String userId, String unitId) {
        Optional<Device> device = deviceRepository.findByUserIdAndUnitId(userId, unitId);
        return device.orElseGet(() -> {
            DeviceConfiguration defaultConfig = DeviceConfiguration.builder()
                    .userId(userId)
                    .unitId(unitId)
                    .creationDate(createReceptionDate())
                    .build();
            DeviceConfiguration saved = deviceConfigurationRepository.save(defaultConfig);
            return deviceRepository.updateConfiguration(userId, unitId, saved);
        });
    }

    public DeviceConfiguration getDeviceConfiguration(String userId, String unitId) {
        return getOrCreateDevice(userId, unitId).getConfiguration();
    }

    public Page<Device> getDevicesByUserId(String userId, Optional<Operator> query, Pageable pageable) {
        return deviceRepository.findByUserId(userId, query, pageable);
    }

    public Page<DeviceEvent> getDeviceEventsByUnitId(String userId, String unitId, boolean onlyChanged, Pageable pageable) {
        return deviceEventRepository.findByUserIdAndUnitId(userId, unitId, onlyChanged, pageable);
    }

    public Device getDeviceByUserIdAndUnitId(String userId, String unitId) {
        return deviceRepository.findByUserIdAndUnitId(userId, unitId).orElseThrow(DeviceNotFoundException::new);
    }
}
