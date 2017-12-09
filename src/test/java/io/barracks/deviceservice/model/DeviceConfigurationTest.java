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

package io.barracks.deviceservice.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.commons.test.JsonResourceLoader;
import net.minidev.json.JSONObject;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class DeviceConfigurationTest {
    @Test
    public void deserializeDeviceConfiguration_shouldIgnoreFields() throws Exception {
        // Given
        final JSONObject json = JsonResourceLoader.getJsonFromResource(getClass(), "configuration");

        // When
        DeviceConfiguration configuration = new ObjectMapper().readValue(json.toJSONString(), DeviceConfiguration.class);

        // Then
        assertThat(configuration.getId()).isNull();
        assertThat(configuration.getUnitId()).isNull();
        assertThat(configuration.getUserId()).isNull();
        assertThat(configuration.getCreationDate()).isNull();
    }

    @Test
    public void serializeDeviceConfiguration_shouldSerializeFields() {
        // Given
        final DeviceConfiguration event = DeviceConfiguration.builder()
                .id(UUID.randomUUID().toString())
                .unitId(UUID.randomUUID().toString())
                .userId(UUID.randomUUID().toString())
                .creationDate(new Date(1234567890L))
                .build();
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DeviceConfiguration.DATE_FORMAT);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Z"));

        // When
        JsonNode node = new ObjectMapper().valueToTree(event);

        // Then
        assertThat(node.get("id").textValue()).isEqualTo(event.getId());
        assertThat(node.get("unitId").textValue()).isEqualTo(event.getUnitId());
        assertThat(node.get("userId").textValue()).isEqualTo(event.getUserId());
        assertThat(node.get("creationDate").textValue()).isEqualTo(simpleDateFormat.format(event.getCreationDate()));
    }
}
