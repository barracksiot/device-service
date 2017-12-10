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

package io.barracks.deviceservice.model.operator;

import io.barracks.deviceservice.model.operator.comparison.*;
import io.barracks.deviceservice.model.operator.logical.AndOperator;
import io.barracks.deviceservice.model.operator.logical.OrOperator;
import org.junit.Test;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

public class OperatorConverterTest {

    @Test
    public void convertEqualOperator() {
        // Given
        final String key = "key";
        final String value = "value";
        final Operator operator = new EqualOperator(key, value);
        final Criteria expected = Criteria.where(key).is(value);

        // When
        final Criteria result = OperatorConverter.toMongoCriteria(operator);

        // Then
        assertThat(result).isEqualToComparingFieldByFieldRecursively(expected);
    }

    @Test
    public void convertNotEqualOperator() {
        // Given
        final String key = "key";
        final String value = "value";
        final Operator operator = new NotEqualOperator(key, value);
        final Criteria expected = Criteria.where(key).ne(value);

        // When
        final Criteria result = OperatorConverter.toMongoCriteria(operator);

        // Then
        assertThat(result).isEqualToComparingFieldByFieldRecursively(expected);
    }

    @Test
    public void convertInOperator() {
        // Given
        final String key = "key";
        final Collection<String> values = Arrays.asList("value1", "value2");
        final Operator operator = new InOperator(key, values);
        final Criteria expected = Criteria.where(key).in(values);

        // When
        final Criteria result = OperatorConverter.toMongoCriteria(operator);

        // Then
        assertThat(result).isEqualToComparingFieldByFieldRecursively(expected);
    }

    @Test
    public void convertNotInOperator() {
        // Given
        final String key = "key";
        final Collection<String> values = Arrays.asList("value1", "value2");
        final Operator operator = new NotInOperator(key, values);
        final Criteria expected = Criteria.where(key).nin(values);

        // When
        final Criteria result = OperatorConverter.toMongoCriteria(operator);

        // Then
        assertThat(result).isEqualToComparingFieldByFieldRecursively(expected);
    }

    @Test
    public void convertGreaterThanOperator() {
        // Given
        final String key = "key";
        final String value = "value";
        final Operator operator = new GreaterThanOperator(key, value);
        final Criteria expected = Criteria.where(key).gt(value);

        // When
        final Criteria result = OperatorConverter.toMongoCriteria(operator);

        // Then
        assertThat(result).isEqualToComparingFieldByFieldRecursively(expected);
    }

    @Test
    public void convertGreaterThanOrEqualOperator() {
        // Given
        final String key = "key";
        final String value = "value";
        final Operator operator = new GreaterThanOrEqualOperator(key, value);
        final Criteria expected = Criteria.where(key).gte(value);

        // When
        final Criteria result = OperatorConverter.toMongoCriteria(operator);

        // Then
        assertThat(result).isEqualToComparingFieldByFieldRecursively(expected);
    }

    @Test
    public void convertLessThanOperator() {
        // Given
        final String key = "key";
        final String value = "value";
        final Operator operator = new LessThanOperator(key, value);
        final Criteria expected = Criteria.where(key).lt(value);

        // When
        final Criteria result = OperatorConverter.toMongoCriteria(operator);

        // Then
        assertThat(result).isEqualToComparingFieldByFieldRecursively(expected);
    }

    @Test
    public void convertLessThanOrEqualOperator() {
        // Given
        final String key = "key";
        final String value = "value";
        final Operator operator = new LessThanOrEqualOperator(key, value);
        final Criteria expected = Criteria.where(key).lte(value);

        // When
        final Criteria result = OperatorConverter.toMongoCriteria(operator);

        // Then
        assertThat(result).isEqualToComparingFieldByFieldRecursively(expected);
    }

    @Test
    public void convertRegexOperator() {
        // Given
        final String key = "key";
        final String value = ".*value.*";
        final Operator operator = new RegexOperator(key, value);
        final Criteria expected = Criteria.where(key).regex(java.util.regex.Pattern.compile(value));

        // When
        final Criteria result = OperatorConverter.toMongoCriteria(operator);

        // Then
        assertThat(result).isEqualToComparingFieldByFieldRecursively(expected);
    }

    @Test
    public void convertAndOperator() {
        // Given
        final String key1 = "key1";
        final String key2 = "key2";
        final String value1 = "value1";
        final String value2 = "value2";
        final Operator operator = new AndOperator(new ArrayList<Operator>() {{
            add(new GreaterThanOperator(key1, value1));
            add(new LessThanOperator(key2, value2));
        }});
        final Criteria[] innerCriteria = {
                Criteria.where(key1).gt(value1),
                Criteria.where(key2).gt(value2)
        };
        final Criteria expected = new Criteria();
        expected.andOperator(innerCriteria);

        // When
        final Criteria result = OperatorConverter.toMongoCriteria(operator);

        // Then
        assertThat(result.toString()).isEqualTo(expected.toString());
    }

    @Test
    public void convertOrOperator() {
        // Given
        final String key1 = "key1";
        final String key2 = "key2";
        final String value1 = "value1";
        final String value2 = "value2.*";
        final Operator operator = new OrOperator(new ArrayList<Operator>() {{
            add(new NotEqualOperator(key1, value1));
            add(new RegexOperator(key2, value2));
        }});
        final Criteria[] innerCriteria = {
                Criteria.where(key1).ne(value1),
                Criteria.where(key2).regex(java.util.regex.Pattern.compile(value2))
        };
        final Criteria expected = new Criteria();
        expected.orOperator(innerCriteria);

        // When
        final Criteria result = OperatorConverter.toMongoCriteria(operator);

        // Then
        assertThat(result.toString()).isEqualTo(expected.toString());
    }
}
