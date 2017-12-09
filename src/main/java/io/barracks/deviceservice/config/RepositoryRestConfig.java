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
import io.barracks.deviceservice.model.Filter;
import io.barracks.deviceservice.model.Segment;
import io.barracks.deviceservice.model.operator.Operator;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableMongoAuditing
public class RepositoryRestConfig extends RepositoryRestConfigurerAdapter {
    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
        config.exposeIdsFor(Segment.class);
    }

    @Bean
    CustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        converters.add(new OperatorToStringConversion(objectMapper));
        converters.add(new StringToOperatorConversion(objectMapper));
        return new CustomConversions(converters);
    }

    @AllArgsConstructor
    static class OperatorToStringConversion implements Converter<Operator, String> {
        private final ObjectMapper objectMapper;

        @Override
        public String convert(Operator source) {
            try {
                return objectMapper.writeValueAsString(source);
            } catch (JsonProcessingException e) {
                throw new SegmentConversionException(e);
            }
        }
    }

    @AllArgsConstructor
    static class StringToOperatorConversion implements Converter<String, Operator> {
        private final ObjectMapper objectMapper;

        @Override
        public Operator convert(String source) {
            try {
                return objectMapper.readValue(source, Operator.class);
            } catch (IOException e) {
                throw new SegmentConversionException(e);
            }
        }
    }
}