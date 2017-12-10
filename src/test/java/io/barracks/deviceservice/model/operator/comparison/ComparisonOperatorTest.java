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

package io.barracks.deviceservice.model.operator.comparison;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.barracks.deviceservice.model.operator.OperatorDeserializer;
import org.junit.Test;
import org.mockito.Mockito;

import javax.annotation.CheckReturnValue;
import javax.annotation.meta.When;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ComparisonOperatorTest {

    @Test
    public void from_shouldReturnExpectedInstance() {
        // Given
        final String key = UUID.randomUUID().toString();
        final String value = UUID.randomUUID().toString();
        final ComparisonOperatorType[] types = {
                ComparisonOperatorType.EQUAL,
                ComparisonOperatorType.NOT_EQUAL,
                ComparisonOperatorType.IN,
                ComparisonOperatorType.NIN,
                ComparisonOperatorType.GREATER_THAN,
                ComparisonOperatorType.GREATER_THAN_OR_EQUAL,
                ComparisonOperatorType.LESS_THAN,
                ComparisonOperatorType.LESS_THAN_OR_EQUAL,
                ComparisonOperatorType.REGEX
        };
        final Object[] expected = {
                new EqualOperator(key, value),
                new NotEqualOperator(key, value),
                new InOperator(key, value),
                new NotInOperator(key, value),
                new GreaterThanOperator(key, value),
                new GreaterThanOrEqualOperator(key, value),
                new LessThanOperator(key, value),
                new LessThanOrEqualOperator(key, value),
                new RegexOperator(key, value),
        };

        // When
        Object[] result = new Object[types.length];
        ComparisonOperatorType[] resultNames = new ComparisonOperatorType[types.length];
        for (int i = 0; i < types.length; i++) {
            result[i] = ComparisonOperator.from(types[i].getName(), key, value);
            resultNames[i] = ((ComparisonOperator) result[i]).getType();
        }

        // Then
        assertThat(result).containsExactly(expected);
        assertThat(resultNames).containsExactly(types);
    }

    @Test
    public void from_whenUnknownOperator_shouldThrowException() {
        // Given
        final String key = UUID.randomUUID().toString();
        final String value = UUID.randomUUID().toString();
        final String name = UUID.randomUUID().toString();

        // Then When
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> ComparisonOperator.from(name, key, value)
        );
    }

    @Test
    public void equal_ifNullValue_shouldCheckIfOtherValueIsNull() {
        // Given
        final String key = UUID.randomUUID().toString();
        final String value = UUID.randomUUID().toString();
        final EqualOperator equal = new EqualOperator(key, null);

        // When
        final boolean result = equal.compare(value);

        // Then
        assertFalse(result);
    }

    @Test
    public void equal_ifBothNull_shouldReturnTrue() {
        // Given
        final String key = UUID.randomUUID().toString();
        final EqualOperator equal = new EqualOperator(key, null);

        // When
        final boolean result = equal.compare(null);

        // Then
        assertTrue(result);
    }

    @Test
    public void equal_ifBothEquals_shouldReturnTrue() {
        // Given
        final String key = UUID.randomUUID().toString();
        final String compared = UUID.randomUUID().toString();
        final EqualsVerifier value = new EqualsVerifier();
        final EqualOperator equal = new EqualOperator(key, value);

        // When
        final boolean result = equal.compare(compared);

        // Then
        assertThat(value.compared).isEqualTo(compared);
        assertTrue(result);
    }


    @Test
    public void notEqual_ifNullValue_shouldCheckIfOtherValueIsNull() {
        // Given
        final String key = UUID.randomUUID().toString();
        final String value = UUID.randomUUID().toString();
        final NotEqualOperator equal = new NotEqualOperator(key, null);

        // When
        final boolean result = equal.compare(value);

        // Then
        assertTrue(result);
    }

    @Test
    public void notEqual_ifBothNull_shouldReturnTrue() {
        // Given
        final String key = UUID.randomUUID().toString();
        final NotEqualOperator equal = new NotEqualOperator(key, null);

        // When
        final boolean result = equal.compare(null);

        // Then
        assertFalse(result);
    }

    @Test
    public void notEqual_ifBothEquals_shouldReturnTrue() {
        // Given
        final String key = UUID.randomUUID().toString();
        final String compared = UUID.randomUUID().toString();
        final EqualsVerifier value = new EqualsVerifier();
        final NotEqualOperator equal = new NotEqualOperator(key, value);

        // When
        final boolean result = equal.compare(compared);

        // Then
        assertThat(value.compared).isEqualTo(compared);
        assertFalse(result);
    }

    @Test
    public void in_ifValueNull_shouldReturnFalse() {
        // Given
        final String key = UUID.randomUUID().toString();
        final String compared = UUID.randomUUID().toString();
        final InOperator in = new InOperator(key, null);

        // When
        final boolean result = in.compare(compared);

        // Then
        assertFalse(result);
    }

    @Test
    public void in_ifValueNotCollection_shouldReturnFalse() {
        // Given
        final String key = UUID.randomUUID().toString();
        final String compared = UUID.randomUUID().toString();
        final InOperator in = new InOperator(key, UUID.randomUUID().toString());

        // When
        final boolean result = in.compare(compared);

        // Then
        assertFalse(result);
    }

    @Test
    public void in_ifValueContainsCompared_shouldReturnTrue() {
        // Given
        final String key = UUID.randomUUID().toString();
        final String compared = UUID.randomUUID().toString();
        final Object[] array = {UUID.randomUUID().toString(), compared};
        final InOperator in = new InOperator(key, array);

        // When
        final boolean result = in.compare(compared);

        // Then
        assertTrue(result);
    }

    @Test
    public void in_ifValueNotContainsCompared_shouldReturnFalse() {
        // Given
        final String key = UUID.randomUUID().toString();
        final String compared = UUID.randomUUID().toString();
        final Object[] array = {UUID.randomUUID().toString()};
        final InOperator in = new InOperator(key, array);

        // When
        final boolean result = in.compare(compared);

        // Then
        assertFalse(result);
    }

    @Test
    public void nin_ifValueNull_shouldReturnTrue() {
        // Given
        final String key = UUID.randomUUID().toString();
        final String compared = UUID.randomUUID().toString();
        final NotInOperator nin = new NotInOperator(key, null);

        // When
        final boolean result = nin.compare(compared);

        // Then
        assertTrue(result);
    }

    @Test
    public void nin_ifValueNotArray_shouldReturnTrue() {
        // Given
        final String key = UUID.randomUUID().toString();
        final String compared = UUID.randomUUID().toString();
        final NotInOperator nin = new NotInOperator(key, UUID.randomUUID().toString());

        // When
        final boolean result = nin.compare(compared);

        // Then
        assertTrue(result);
    }

    @Test
    public void nin_ifValueContainsCompared_shouldReturnFalse() {
        // Given
        final String key = UUID.randomUUID().toString();
        final String compared = UUID.randomUUID().toString();
        final Object[] array = {UUID.randomUUID().toString(), compared};
        final NotInOperator nin = new NotInOperator(key, array);

        // When
        final boolean result = nin.compare(compared);

        // Then
        assertFalse(result);
    }

    @Test
    public void nin_ifValueNotContainsCompared_shouldReturnTrue() {
        // Given
        final String key = UUID.randomUUID().toString();
        final String compared = UUID.randomUUID().toString();
        final Object[] array = {UUID.randomUUID().toString()};
        final NotInOperator nin = new NotInOperator(key, array);

        // When
        final boolean result = nin.compare(compared);

        // Then
        assertTrue(result);
    }

    @Test
    public void regex_whenValueNotString_shouldReturnFalse() {
        // Given
        final String key = UUID.randomUUID().toString();
        final Object compared = new Object();
        final RegexOperator regex = new RegexOperator(key, UUID.randomUUID().toString());

        // When
        final boolean result = regex.compare(compared);

        // Then
        assertFalse(result);
    }

    @Test
    public void regex_whenValueNull_shouldReturnFalse() {
        // Given
        final String key = UUID.randomUUID().toString();
        final String compared = null;
        final RegexOperator regex = new RegexOperator(key, UUID.randomUUID().toString());

        // When
        final boolean result = regex.compare(compared);

        // Then
        assertFalse(result);
    }

    @Test
    public void regex_whenPatternNotString_shouldReturnFalse() {
        // Given
        final String key = UUID.randomUUID().toString();
        final String compared = UUID.randomUUID().toString();
        final RegexOperator regex = new RegexOperator(key, new Object());

        // When
        final boolean result = regex.compare(compared);

        // Then
        assertFalse(result);
    }

    @Test
    public void regex_whenPatternNull_shouldReturnFalse() {
        // Given
        final String key = UUID.randomUUID().toString();
        final String compared = UUID.randomUUID().toString();
        final RegexOperator regex = new RegexOperator(key, null);

        // When
        final boolean result = regex.compare(compared);

        // Then
        assertFalse(result);
    }

    @Test
    public void regex_whenPatternInvalid_shouldReturnFalse() {
        // Given
        final String key = UUID.randomUUID().toString();
        final String compared = UUID.randomUUID().toString();
        final RegexOperator regex = new RegexOperator(key, "^(ABC$");

        // When
        final boolean result = regex.compare(compared);

        // Then
        assertFalse(result);
    }

    @Test
    public void regex_whenPatternNotMatching_shouldReturnFalse() {
        // Given
        final String key = UUID.randomUUID().toString();
        final String compared = UUID.randomUUID().toString();
        final RegexOperator regex = new RegexOperator(key, "^\\(ABC$");

        // When
        final boolean result = regex.compare(compared);

        // Then
        assertFalse(result);
    }

    @Test
    public void regex_whenPatternMatching_shouldReturnTrue() {
        // Given
        final String key = UUID.randomUUID().toString();
        final String compared = UUID.randomUUID().toString();
        final RegexOperator regex = new RegexOperator(key, "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
        // When
        final boolean result = regex.compare(compared);

        // Then
        assertTrue(result);
    }

    @Test
    public void comparable_whenValueNull_shouldReturnFalse() {
        // Given
        final String key = UUID.randomUUID().toString();
        final UUID compared = UUID.randomUUID();
        final ComparableOperatorVerifier verifier = spy(new ComparableOperatorVerifier(key, null));

        // When
        final boolean result = verifier.compare(compared);

        // Then
        verify(verifier, Mockito.times(0)).computeResult(anyInt());
        assertFalse(result);
    }

    @Test
    public void comparable_whenValueNotComparable_shouldReturnFalse() {
        // Given
        final String key = UUID.randomUUID().toString();
        final UUID compared = UUID.randomUUID();
        final ComparableOperatorVerifier verifier = spy(new ComparableOperatorVerifier(key, new Object()));

        // When
        final boolean result = verifier.compare(compared);

        // Then
        verify(verifier, Mockito.times(0)).computeResult(anyInt());
        assertFalse(result);
    }

    @Test
    public void comparable_whenComparedNull_shouldReturnFalse() {
        // Given
        final String key = UUID.randomUUID().toString();
        final ComparableOperatorVerifier verifier = spy(new ComparableOperatorVerifier(key, UUID.randomUUID()));

        // When
        final boolean result = verifier.compare(null);

        // Then
        verify(verifier, Mockito.times(0)).computeResult(anyInt());
        assertFalse(result);
    }

    @Test
    public void comparable_whenComparedNotComparable_shouldReturnFalse() {
        // Given
        final String key = UUID.randomUUID().toString();
        final Object compared = new Object();
        final ComparableOperatorVerifier verifier = spy(new ComparableOperatorVerifier(key, UUID.randomUUID()));

        // When
        final boolean result = verifier.compare(compared);

        // Then
        verify(verifier, Mockito.times(0)).computeResult(anyInt());
        assertFalse(result);
    }


    @Test
    public void comparable_whenValueAndComparedNotCompatible_shouldReturnFalse() {
        // Given
        final String key = UUID.randomUUID().toString();
        final String compared = UUID.randomUUID().toString();
        final ComparableOperatorVerifier verifier = spy(new ComparableOperatorVerifier(key, UUID.randomUUID()));

        // When
        final boolean result = verifier.compare(compared);

        // Then
        verify(verifier, Mockito.times(0)).computeResult(anyInt());
        assertFalse(result);
    }

    @Test
    public void comparable_whenValueAndComparableNotEqual_shouldReturnFalse() {
        // Given
        final String key = UUID.randomUUID().toString();
        final UUID compared = UUID.randomUUID();
        final ComparableOperatorVerifier verifier = spy(new ComparableOperatorVerifier(key, UUID.randomUUID()));

        // When
        final boolean result = verifier.compare(compared);

        // Then
        verify(verifier, Mockito.times(1)).computeResult(not(eq(0)));
        assertFalse(result);
    }

    @Test
    public void comparable_whenValueAndComparableEqual_shouldReturnTrue() {
        // Given
        final String key = UUID.randomUUID().toString();
        final UUID compared = UUID.randomUUID();
        final ComparableOperatorVerifier verifier = spy(new ComparableOperatorVerifier(key, compared));

        // When
        final boolean result = verifier.compare(compared);

        // Then
        verify(verifier, Mockito.times(1)).computeResult(eq(0));
        assertTrue(result);
    }

    @Test
    public void greaterThan_whenGreater_returnTrue() {
        // Given
        final String key = UUID.randomUUID().toString();
        final int value = 1;
        final GreaterThanOperator greaterThan = new GreaterThanOperator(key, value - 1);

        // When
        final boolean result = greaterThan.compare(value);

        // Then
        assertTrue(result);
    }

    @Test
    public void greaterThan_whenEqual_returnFalse() {
        // Given
        final String key = UUID.randomUUID().toString();
        final int value = 1;
        final GreaterThanOperator greaterThan = new GreaterThanOperator(key, value);

        // When
        final boolean result = greaterThan.compare(value);

        // Then
        assertFalse(result);
    }

    @Test
    public void greaterThan_whenLess_returnFalse() {
        // Given
        final String key = UUID.randomUUID().toString();
        final int value = 1;
        final GreaterThanOperator greaterThan = new GreaterThanOperator(key, value + 1);

        // When
        final boolean result = greaterThan.compare(value);

        // Then
        assertFalse(result);
    }

    @Test
    public void greaterThanOrEqual_whenGreater_returnTrue() {
        // Given
        final String key = UUID.randomUUID().toString();
        final int value = 1;
        final GreaterThanOrEqualOperator greaterThanOrEqual = new GreaterThanOrEqualOperator(key, value - 1);

        // When
        final boolean result = greaterThanOrEqual.compare(value);

        // Then
        assertTrue(result);
    }

    @Test
    public void greaterThanOrEqual_whenEqual_returnTrue() {
        // Given
        final String key = UUID.randomUUID().toString();
        final int value = 1;
        final GreaterThanOrEqualOperator greaterThanOrEqual = new GreaterThanOrEqualOperator(key, value);

        // When
        final boolean result = greaterThanOrEqual.compare(value);

        // Then
        assertTrue(result);
    }

    @Test
    public void greaterThanOrEqual_whenLess_returnFalse() {
        // Given
        final String key = UUID.randomUUID().toString();
        final int value = 1;
        final GreaterThanOrEqualOperator greaterThanOrEqual = new GreaterThanOrEqualOperator(key, value + 1);

        // When
        final boolean result = greaterThanOrEqual.compare(value);

        // Then
        assertFalse(result);
    }

    @Test
    public void lessThan_whenGreater_returnFalse() {
        // Given
        final String key = UUID.randomUUID().toString();
        final int value = 1;
        final LessThanOperator lessThan = new LessThanOperator(key, value - 1);

        // When
        final boolean result = lessThan.compare(value);

        // Then
        assertFalse(result);
    }

    @Test
    public void lessThan_whenEqual_returnFalse() {
        // Given
        final String key = UUID.randomUUID().toString();
        final int value = 1;
        final LessThanOperator lessThan = new LessThanOperator(key, value);

        // When
        final boolean result = lessThan.compare(value);

        // Then
        assertFalse(result);
    }

    @Test
    public void lessThan_whenLess_returnTrue() {
        // Given
        final String key = UUID.randomUUID().toString();
        final int value = 1;
        final LessThanOperator lessThan = new LessThanOperator(key, value + 1);

        // When
        final boolean result = lessThan.compare(value);

        // Then
        assertTrue(result);
    }

    @Test
    public void lessThanOrEqual_whenGreater_returnFalse() {
        // Given
        final String key = UUID.randomUUID().toString();
        final int value = 1;
        final LessThanOrEqualOperator lessThanOrEqual = new LessThanOrEqualOperator(key, value - 1);

        // When
        final boolean result = lessThanOrEqual.compare(value);

        // Then
        assertFalse(result);
    }

    @Test
    public void lessThanOrEqual_whenEqual_returnTrue() {
        // Given
        final String key = UUID.randomUUID().toString();
        final int value = 1;
        final LessThanOrEqualOperator lessThanOrEqual = new LessThanOrEqualOperator(key, value);

        // When
        final boolean result = lessThanOrEqual.compare(value);

        // Then
        assertTrue(result);
    }

    @Test
    public void lessThanOrEqual_whenLess_returnTrue() {
        // Given
        final String key = UUID.randomUUID().toString();
        final int value = 1;
        final LessThanOrEqualOperator lessThanOrEqual = new LessThanOrEqualOperator(key, value + 1);

        // When
        final boolean result = lessThanOrEqual.compare(value);

        // Then
        assertTrue(result);
    }

    @Test
    public void prepareKey_shouldReplaceUserKeysWithDomainKeys() {
        // Given
        final String[] keys = new String[]{
                "unitId",
                "lastSeen",
                "customClientData",
                "customClientData.simple",
                "customClientData.less.simple"
        };
        final String[] expected = new String[]{
                "unitId",
                "receptionDate",
                "request.customClientData",
                "request.customClientData.simple",
                "request.customClientData.less.simple"
        };

        // Then when
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            ComparisonOperator operator = ComparisonOperator.from("eq", key, new Object());
            assertThat(operator.prepareKey()).isEqualTo(expected[i]);
        }
    }

    @Test
    public void matches_shouldLookForValueinJsonTree_andReturnCompareResult() throws Exception {
        // Given
        final JsonNode node = new ObjectMapper().readTree("{\"test\":{\"nested\":{\"values\":42, \"nulls\":null}}}");
        final String[] keys = new String[]{
                "test",
                "test.nested",
                "test.nested.values",
                "test.nested.nulls",
                "test.nothere",
                "whatever.you.want"
        };
        final JsonNode[] expectedToParse = new JsonNode[]{
                node.get("test"),
                node.get("test").get("nested"),
                node.get("test").get("nested").get("values"),
                null,
                null,
                null
        };

        for (int i = 0; i < keys.length; i++) {
            final ComparisonOperator operator = mock(ComparisonOperator.class);
            when(operator.matches(any())).thenCallRealMethod();
            doReturn(keys[i]).when(operator).prepareKey();
            final Object expectedParsing = OperatorDeserializer.parseValue(expectedToParse[i]);
            doReturn(true).when(operator).compare(expectedParsing);

            // When
            final boolean result = operator.matches(node);

            // Then
            verify(operator).compare(expectedParsing);
            assertTrue(result);
        }
    }

    private static class ComparableOperatorVerifier extends ComparableOperator {

        ComparableOperatorVerifier(String key, Object value) {
            super(key, value);
        }

        @Override
        @CheckReturnValue(when = When.NEVER)
        protected boolean computeResult(int result) {
            return result == 0;
        }

        @Override
        public ComparisonOperatorType getType() {
            return null;
        }
    }

    private static final class EqualsVerifier {
        private Object compared;

        @SuppressFBWarnings(value = {"HE_EQUALS_USE_HASHCODE", "EQ_UNUSUAL"}, justification = "Stub")
        @Override
        public boolean equals(Object obj) {
            this.compared = obj;
            return true;
        }

        @Override
        public int hashCode() {
            return 42;
        }
    }
}
