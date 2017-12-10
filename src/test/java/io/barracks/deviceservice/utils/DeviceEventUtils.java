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

package io.barracks.deviceservice.utils;

import io.barracks.deviceservice.model.DeviceEvent;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class DeviceEventUtils {

    public static DeviceEvent getDeviceEvent() {
        return getDeviceEvent(UUID.randomUUID().toString());
    }

    public static DeviceEvent getDeviceEvent(String userId) {
        return getDeviceEvent(userId, UUID.randomUUID().toString());
    }

    public static DeviceEvent getDeviceEvent(String userId, String unitId) {
        final DeviceEvent event = DeviceEvent.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .unitId(unitId)
                .changed(true)
                .receptionDate(OffsetDateTime.now(ZoneOffset.UTC))
                .request(DeviceRequestUtils.getDeviceRequest())
                .response(DevicePackageResponseUtils.getDevicePackageResponse())
                .build();
        assertThat(event).hasNoNullFieldsOrProperties();
        return event;
    }
}
