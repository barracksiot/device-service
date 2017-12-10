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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.barracks.deviceservice.model.operator.comparison.ComparisonOperator;
import io.barracks.deviceservice.model.operator.logical.LogicalOperator;

import java.io.IOException;

public class OperatorSerializer extends JsonSerializer<Operator> {
    @Override
    public void serialize(Operator value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
        writeOperator(value, gen, serializers);
    }

    private void writeOperator(Operator operator, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (operator instanceof LogicalOperator) {
            writeLogicalOperator((LogicalOperator) operator, gen, serializers);
        } else {
            writeComparisonOperator((ComparisonOperator) operator, gen, serializers);
        }
    }

    private void writeLogicalOperator(LogicalOperator operator, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeArrayFieldStart(operator.getType().getName());
        for (Operator operand : operator.getOperands()) {
            writeOperator(operand, gen, serializers);
        }
        gen.writeEndArray();
        gen.writeEndObject();
    }

    private void writeComparisonOperator(ComparisonOperator operator, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeFieldName(operator.getType().getName());
        gen.writeStartObject();
        serializers.defaultSerializeField(operator.getKey(), operator.getValue(), gen);
        gen.writeEndObject();
        gen.writeEndObject();
    }
}
