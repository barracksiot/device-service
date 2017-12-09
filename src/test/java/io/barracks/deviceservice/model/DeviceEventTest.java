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

public class DeviceEventTest {
    @Test
    public void deserializeEvent_shouldIgnoreFields() throws Exception {
        // Given
        final JSONObject json = JsonResourceLoader.getJsonFromResource(getClass(), "event");

        // When
        DeviceEvent event = new ObjectMapper().readValue(json.toJSONString(), DeviceEvent.class);

        // Then
        assertThat(event.getId()).isNull();
        assertThat(event.getSegmentId()).isNull();
        assertThat(event.isChanged()).isFalse();
        assertThat(event.getReceptionDate()).isNull();
    }

    @Test
    public void serializeEvent_shouldSerializeFields() {
        // Given
        final DeviceEvent event = DeviceEvent.builder()
                .id(UUID.randomUUID().toString())
                .changed(true)
                .receptionDate(new Date(1234567890L))
                .segmentId(UUID.randomUUID().toString())
                .build();
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DeviceEvent.DATE_FORMAT);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Z"));

        // When
        JsonNode node = new ObjectMapper().valueToTree(event);

        // Then
        assertThat(event.getId()).isEqualTo(node.get("id").textValue());
        assertThat(event.getSegmentId()).isEqualTo(node.get("segmentId").textValue());
        assertThat(event.isChanged()).isEqualTo(node.get("changed").booleanValue());
        assertThat(simpleDateFormat.format(event.getReceptionDate())).isEqualTo(node.get("receptionDate").textValue());
    }
}
