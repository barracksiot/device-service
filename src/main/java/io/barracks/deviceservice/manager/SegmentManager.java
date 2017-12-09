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

import io.barracks.deviceservice.manager.exception.InvalidSegmentOrderException;
import io.barracks.deviceservice.manager.exception.SegmentNotFoundException;
import io.barracks.deviceservice.model.Device;
import io.barracks.deviceservice.model.Segment;
import io.barracks.deviceservice.model.SegmentOrder;
import io.barracks.deviceservice.repository.DeviceRepository;
import io.barracks.deviceservice.repository.SegmentOrderRepository;
import io.barracks.deviceservice.repository.SegmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SegmentManager {
    private final SegmentRepository segmentRepository;
    private final DeviceRepository deviceRepository;
    private final SegmentOrderRepository segmentOrderRepository;

    @Autowired
    public SegmentManager(SegmentRepository segmentRepository, DeviceRepository deviceRepository, SegmentOrderRepository segmentOrderRepository) {
        this.segmentRepository = segmentRepository;
        this.deviceRepository = deviceRepository;
        this.segmentOrderRepository = segmentOrderRepository;
    }

    public Segment createSegment(Segment segment) {
        return segmentRepository.insert(segment);
    }

    public Segment getSegmentById(String id) {
        return Optional.ofNullable(segmentRepository.findOne(id)).orElseThrow(() -> new SegmentNotFoundException(id));
    }

    public Segment updateSegment(String id, Segment segment) {
        Segment toSave = segment.toBuilder()
                .id(id)
                .updated(null)
                .build();
        return segmentRepository.save(toSave);
    }

    public Page<Device> getDevicesBySegmentId(String segmentId, Pageable pageable) {
        return deviceRepository.findBySegmentId(segmentId, pageable);
    }

    public Page<Device> getOtherDevicesForUser(String userId, Pageable pageable) {
        SegmentOrder order = segmentOrderRepository.findByUserId(userId);
        return deviceRepository.findDevicesNotIn(userId, order.getSegmentIds(), pageable);
    }

    public List<Segment> getActiveSegments(String userId) {
        return segmentRepository.getSegmentsInIds(userId, segmentOrderRepository.findByUserId(userId).getSegmentIds());
    }

    public List<Segment> getInactiveSegments(String userId) {
        return segmentRepository.getSegmentsNotInIds(userId, segmentOrderRepository.findByUserId(userId).getSegmentIds());
    }

    public List<String> updateSegmentOrder(String userId, List<String> order) {
        List<Segment> segments = segmentRepository.getSegmentsInIds(userId, order);
        if (segments.size() != order.size()) {
            throw new InvalidSegmentOrderException("Invalid segments order '" + order + "' for user '" + userId + "'");
        }
        return segmentOrderRepository.updateOrder(userId, order).getSegmentIds();
    }

    public Page<Device> getDevicesBySegmentIdAndVersionId(String segmentId, String versionId, Pageable pageable) {
        return deviceRepository.findBySegmentIdAndVersionId(segmentId, versionId, pageable);
    }

    public Page<Device> getOtherDevicesForUserAndVersionId(String userId, String versionId, Pageable pageable) {
        SegmentOrder order = segmentOrderRepository.findByUserId(userId);
        return deviceRepository.findForUserIdAndVersionIdAndNotSegmentIds(userId, versionId, order.getSegmentIds(), pageable);
    }
}
