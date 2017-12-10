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

package io.barracks.deviceservice.model.json;

import io.barracks.deviceservice.model.Device;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.OffsetDateTime;

import static io.barracks.deviceservice.utils.DeviceUtils.getDevice;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class DeviceJsonTest {

    @Autowired
    private JacksonTester<Device> json;

    @Test
    public void serializeShouldIgnoreId() throws Exception {
        // Given
        final Device device = getDevice();

        // When
        final JsonContent<Device> result = json.write(device);

        // Then
        assertThat(result).doesNotHaveJsonPathValue("id");
        assertThat(result).extractingJsonPathValue("userId").isEqualTo(device.getUserId());
        assertThat(result).extractingJsonPathValue("unitId").isEqualTo(device.getUnitId());
        assertThat(result).extractingJsonPathStringValue("firstSeen").satisfies(value -> OffsetDateTime.parse(value).isEqual(device.getFirstSeen().get()));
        assertThat(result).doesNotHaveEmptyJsonPathValue("lastEvent");
    }

}
