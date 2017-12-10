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

import io.barracks.deviceservice.model.DataSet;
import io.barracks.deviceservice.repository.DeviceEventRepository;
import io.barracks.deviceservice.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class StatsManager {

    private final DeviceRepository deviceRepository;
    private final DeviceEventRepository deviceEventRepository;

    @Autowired
    public StatsManager(DeviceRepository deviceRepository, DeviceEventRepository deviceEventRepository) {
        this.deviceRepository = deviceRepository;
        this.deviceEventRepository = deviceEventRepository;
    }

    public DataSet getLastSeenDeviceCount(String userId, OffsetDateTime start, OffsetDateTime end) {
        return deviceRepository.getLastSeenDeviceCount(userId, start, end);
    }

    public DataSet getSeenDeviceCount(String userId, OffsetDateTime start, OffsetDateTime end) {
        return deviceEventRepository.getSeenDeviceCount(userId, start, end);
    }

    public DataSet getDeviceCountPerUserId() {
        return deviceRepository.getDeviceCountPerUserId();
    }
}
