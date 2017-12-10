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

package io.barracks.deviceservice.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.deviceservice.model.operator.Operator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@JsonTest
public class MongoConfigTest {
    @SpyBean
    private ObjectMapper mapper;

    @Autowired
    private JacksonTester<Operator> json;

    @Value("classpath:io/barracks/deviceservice/query.json")
    private Resource query;

    @Test
    public void customConversions_ShouldContainOperatorConverters() throws Exception {
        // Given
        final MongoConfig mongoConfig = new MongoConfig(mapper, mock(MongoDbFactory.class), mock(MongoMappingContext.class));

        // When
        final CustomConversions customConversions = mongoConfig.customConversions();

        // Then
        assertTrue(customConversions.hasCustomWriteTarget(Operator.class, String.class));
        assertTrue(customConversions.hasCustomReadTarget(String.class, Operator.class));
    }

    @Test
    public void convertOperatorToString_shouldUseObjectMapper() throws Exception {
        // Given
        final Operator source = json.readObject(query);
        final String expected = UUID.randomUUID().toString();
        doReturn(expected).when(mapper).writeValueAsString(source);

        // When
        final String result = new MongoConfig.OperatorToStringConversion(mapper)
                .convert(source);

        // Then
        verify(mapper).writeValueAsString(source);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void convertStringToOperator_whenObjectMapperFails_shouldThrowException() throws Exception {
        // Given
        final Operator source = json.readObject(query);
        doThrow(JsonProcessingException.class).when(mapper).writeValueAsString(source);

        // Then When
        assertThatExceptionOfType(OperatorConversionException.class)
                .isThrownBy(() -> new MongoConfig.OperatorToStringConversion(mapper).convert(source))
                .withCauseInstanceOf(JsonProcessingException.class);
    }

    @Test
    public void convertStringToOperator_shouldUseObjectMapper() throws Exception {
        // Given
        final Operator expected = json.readObject(query);
        final String source = UUID.randomUUID().toString();
        doReturn(expected).when(mapper).readValue(source, Operator.class);

        // When
        final Operator result = new MongoConfig.StringToOperatorConversion(mapper)
                .convert(source);

        // Then
        verify(mapper).readValue(source, Operator.class);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void convertOperatorToString_whenObjectMapperFails_shouldThrowException() throws Exception {
        // Given
        final String source = UUID.randomUUID().toString();
        doThrow(IOException.class).when(mapper).readValue(source, Operator.class);

        // Then When
        assertThatExceptionOfType(OperatorConversionException.class)
                .isThrownBy(() -> new MongoConfig.StringToOperatorConversion(mapper).convert(source))
                .withCauseInstanceOf(IOException.class);
    }
}
