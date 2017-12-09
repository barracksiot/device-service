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
import io.barracks.commons.test.ServiceClientTest;
import io.barracks.deviceservice.model.operator.Operator;
import io.barracks.deviceservice.utils.FilterUtils;
import net.minidev.json.JSONObject;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FilterTest extends ServiceClientTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void deserialize_shouldIgnoreDatesAndIds() throws Exception {
        // Given
        final JSONObject source = getJsonFromResource("valid");
        final Filter expected = Filter.builder()
                .name(source.getAsString("name"))
                .query(objectMapper.readValue(source.getAsString("query"), Operator.class))
                .build();

        // When
        final Filter result = objectMapper.readValue(source.toJSONString(), Filter.class);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void serialize_shouldIncludeNameAndQuery() {
        // Given
        final Filter source = FilterUtils.getFilter();

        // When
        final JsonNode result = objectMapper.valueToTree(source);

        // Then
        assertThat(result.get("name").asText()).isEqualTo(source.getName());
        assertThat(result.get("query")).isEqualTo(objectMapper.valueToTree(source.getQuery()));
        assertThat(result.get("id")).isNull();
        assertThat(result.get("userId")).isNull();
        assertThat(result.get("created")).isNull();
        assertThat(result.get("updated")).isNull();
    }
}
