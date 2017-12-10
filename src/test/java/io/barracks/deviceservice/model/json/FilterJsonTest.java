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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.deviceservice.model.Filter;
import io.barracks.deviceservice.utils.FilterUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class FilterJsonTest {
    @Autowired
    private JacksonTester<Filter> json;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("classpath:io/barracks/deviceservice/filter.json")
    private Resource filter;

    @Test
    public void deserialize_shouldIgnoreDatesAndIds() throws Exception {
        // Given
        final Filter expected = Filter.builder()
                .name("A valid filter name")
                .build();

        // When
        final Filter result = json.readObject(filter.getInputStream());

        // Then
        assertThat(result).isEqualToIgnoringGivenFields(expected, "query");
        assertThat(result.getQuery()).isNotNull();
    }

    @Test
    public void serialize_shouldIncludeNameAndQuery() throws Exception {
        // Given
        final Filter source = FilterUtils.getFilter();

        // When
        final JsonContent<Filter> result = json.write(source);

        // Then
        assertThat(result).extractingJsonPathStringValue("name").isEqualTo(source.getName());
        assertThat(result).hasJsonPathValue("query");
        assertThat(result).doesNotHaveJsonPathValue("id");
        assertThat(result).doesNotHaveJsonPathValue("userId");
        assertThat(result).doesNotHaveJsonPathValue("created");
        assertThat(result).doesNotHaveJsonPathValue("updated");
    }
}
