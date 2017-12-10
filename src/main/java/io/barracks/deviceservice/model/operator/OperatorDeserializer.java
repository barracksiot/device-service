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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.barracks.deviceservice.model.operator.comparison.ComparisonOperator;
import io.barracks.deviceservice.model.operator.logical.LogicalOperator;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

public class OperatorDeserializer extends JsonDeserializer<Operator> {

    public static Object parseValue(@Nullable JsonNode node) {
        if (node == null) {
            return null;
        }
        switch (node.getNodeType()) {
            case ARRAY:
                List<Object> list = new ArrayList<>(node.size());
                for (int itemIdx = 0; itemIdx < node.size(); itemIdx++) {
                    list.add(parseValue(node.get(itemIdx)));
                }
                return list;
            case OBJECT:
                Map<String, Object> object = new LinkedHashMap<>();
                for (Iterator<String> iterator = node.fieldNames(); iterator.hasNext(); ) {
                    String field = iterator.next();
                    Object value = parseValue(node.get(field));
                    object.put(field, value);
                }
                return object;
            case BOOLEAN:
                return node.booleanValue();
            case NUMBER:
                return node.numberValue();
            case STRING:
                return node.textValue();
            case NULL:
            default:
                return null;
        }
    }

    @Override
    public Operator deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        return getOperator(node);
    }

    private Operator getOperator(JsonNode node) throws IOException {
        if (node.size() != 1) {
            throw new IOException("Logical operator has to hold exactly one field.");
        }
        Map.Entry<String, JsonNode> field = node.fields().next();
        if (field.getValue().isArray()) {
            return getLogicalOperator(field.getKey(), (ArrayNode) field.getValue());
        } else if (field.getValue().isObject()) {
            return getComparisonOperator(field.getKey(), (ObjectNode) field.getValue());
        } else {
            throw new IOException("Can't parse JSON Node, not an array or object");
        }
    }

    private Operator getLogicalOperator(String name, ArrayNode node) throws IOException {
        List<Operator> children = new ArrayList<>(node.size());
        for (Iterator<JsonNode> iter = node.elements(); iter.hasNext(); ) {
            children.add(getOperator(iter.next()));
        }
        return LogicalOperator.from(name, children);
    }

    private Operator getComparisonOperator(String name, ObjectNode node) throws IOException {
        if (node.size() != 1) {
            throw new IOException("Comparison operator has to hold exactly one field.");
        }
        Map.Entry<String, JsonNode> entry = node.fields().next();
        return ComparisonOperator.from(name, entry.getKey(), parseValue(entry.getValue()));
    }
}
