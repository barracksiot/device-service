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

package io.barracks.deviceservice.model.operator.logical;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.deviceservice.model.operator.Operator;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class LogicalOperatorTest {
    @Test
    public void and_whenAtLeastOneOperandIsFalse_shouldReturnFalse() throws Exception {
        // Given
        final JsonNode device = new ObjectMapper().readTree("{}");
        final List<Operator> operands = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Operator operator = mock(Operator.class);
            doReturn(true).when(operator).matches(device);
            operands.add(operator);
        }
        doReturn(false).when(operands.get(5)).matches(device);
        final AndOperator and = new AndOperator(operands);

        // When
        final boolean result = and.matches(device);

        // Then
        assertFalse(result);
    }

    @Test
    public void and_whenAllOperandsAreTrue_shouldReturnTrue() throws Exception {
        // Given
        final JsonNode device = new ObjectMapper().readTree("{}");
        final List<Operator> operands = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Operator operator = mock(Operator.class);
            doReturn(true).when(operator).matches(device);
            operands.add(operator);
        }
        final AndOperator and = new AndOperator(operands);

        // When
        final boolean result = and.matches(device);

        // Then
        assertTrue(result);
    }

    @Test
    public void or_whenAtLestOneOperandIsTrue_shouldReturnTrue() throws Exception {
        // Given
        final JsonNode device = new ObjectMapper().readTree("{}");
        final List<Operator> operands = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Operator operator = mock(Operator.class);
            doReturn(false).when(operator).matches(device);
            operands.add(operator);
        }
        doReturn(true).when(operands.get(5)).matches(device);
        final OrOperator or = new OrOperator(operands);

        // When
        final boolean result = or.matches(device);

        // Then
        assertTrue(result);
    }

    @Test
    public void or_whenAllOperandsAreFalse_shouldReturnFalse() throws Exception {
        // Given
        final JsonNode device = new ObjectMapper().readTree("{}");
        final List<Operator> operands = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Operator operator = mock(Operator.class);
            doReturn(false).when(operator).matches(device);
            operands.add(operator);
        }
        final OrOperator or = new OrOperator(operands);

        // When
        final boolean result = or.matches(device);

        // Then
        assertFalse(result);
    }
}
