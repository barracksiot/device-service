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

package io.barracks.deviceservice.rest;

import io.barracks.deviceservice.manager.StatsManager;
import io.barracks.deviceservice.model.DataSet;
import io.barracks.deviceservice.rest.entity.DateRange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats")
public class StatsResource {

    private final StatsManager statsManager;

    @Autowired
    public StatsResource(StatsManager statsManager) {
        this.statsManager = statsManager;
    }

    @RequestMapping("/{userId}/devices/lastSeen")
    public DataSet getAliveDevicesCount(
            @PathVariable("userId") String userId,
            @Validated() @ModelAttribute DateRange dateRange
    ) {
        return statsManager.getLastSeenDeviceCount(userId, dateRange.getStart(), dateRange.getEnd());
    }

    @RequestMapping("/{userId}/devices/seen")
    public DataSet getUniqueDevicesCount(
            @PathVariable("userId") String userId,
            @Validated() @ModelAttribute DateRange dateRange
    ) {
        return statsManager.getSeenDeviceCount(userId, dateRange.getStart(), dateRange.getEnd());
    }

    @RequestMapping("/devices/perUserId")
    public DataSet getDeviceCountPerUserId() {
        return statsManager.getDeviceCountPerUserId();
    }
}
