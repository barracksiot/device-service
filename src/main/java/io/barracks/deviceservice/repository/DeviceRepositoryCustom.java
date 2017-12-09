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
import io.barracks.deviceservice.model.DeviceConfiguration;
import io.barracks.deviceservice.model.DeviceEvent;
import io.barracks.deviceservice.model.operator.Operator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface DeviceRepositoryCustom {
    Device updateConfiguration(String userId, String unitId, DeviceConfiguration configuration);

    Device updateDeviceEvent(String userId, String unitId, DeviceEvent event);

    Device updateFirstSeen(String userId, String unitId, Date creationDate);

    DataSet getDevicesCountPerVersionId(String userId);

    DataSet getLastSeenDeviceCount(String userId, OffsetDateTime start, OffsetDateTime end);

    DataSet getDeviceCountPerUserId();

    Page<Device> findByUserId(String userId, Optional<Operator> searchFilter, Pageable pageable);

    Page<Device> findBySegmentId(String segmentId, Pageable pageable);

    Page<Device> findDevicesNotIn(String userId, List<String> segmentIds, Pageable pageable);

    Page<Device> findBySegmentIdAndVersionId(String segmentId, String versionId, Pageable pageable);

    Page<Device> findForUserIdAndVersionIdAndNotSegmentIds(String userId, String versionId, List<String> segmentIds, Pageable pageable);
}