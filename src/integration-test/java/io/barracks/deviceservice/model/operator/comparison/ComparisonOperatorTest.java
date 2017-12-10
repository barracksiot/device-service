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
import io.barracks.deviceservice.model.Device;
import io.barracks.deviceservice.model.DeviceEvent;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ComparisonOperatorTest {
    @Test
    public void compareDevice_shouldCompareWithExpectedValue() {
        // Given
        final String unitId = UUID.randomUUID().toString();
        final Device device = Device.builder()
                .unitId(unitId)
                .lastEvent(
                        DeviceEvent.builder().build()
                ).build();
        final ComparisonOperator operator = mock(ComparisonOperator.class);
        final JsonNode json = new ObjectMapper().valueToTree(device);
        doCallRealMethod().when(operator).matches(any());
        doCallRealMethod().when(operator).prepareKey();
        when(operator.getKey()).thenReturn("unitId");
        doReturn(true).when(operator).compare(unitId);

        // When
        final boolean result = operator.matches(json);

        // Then
        verify(operator).compare(unitId);
        assertTrue(result);
    }
}
