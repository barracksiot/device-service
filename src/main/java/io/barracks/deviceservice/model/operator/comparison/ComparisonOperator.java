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
import io.barracks.deviceservice.model.operator.Operator;
import io.barracks.deviceservice.model.operator.OperatorDeserializer;
import lombok.*;

import javax.validation.constraints.Pattern;
import java.util.StringTokenizer;
import java.util.regex.Matcher;

@Getter
@EqualsAndHashCode
@ToString
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ComparisonOperator implements Operator {

    static final String KEY_PATTERN = "^(unitId|firstSeen|lastSeen|customClientData(\\..+)?)$";

    private static final String[] patterns = new String[]{
            "^(customClientData)(\\..+)?$",
            "^lastSeen$"
    };

    private static final String[] replacements = new String[]{
            "request.customClientData$2",
            "receptionDate",
    };

    @Pattern(regexp = KEY_PATTERN)
    private final String key;

    private final Object value;

    public static ComparisonOperator from(String name, String key, Object value) {
        ComparisonOperatorType type = ComparisonOperatorType.forName(name);
        switch (type) {
            case EQUAL:
                return new EqualOperator(key, value);
            case NOT_EQUAL:
                return new NotEqualOperator(key, value);
            case IN:
                return new InOperator(key, value);
            case NIN:
                return new NotInOperator(key, value);
            case GREATER_THAN:
                return new GreaterThanOperator(key, value);
            case LESS_THAN:
                return new LessThanOperator(key, value);
            case GREATER_THAN_OR_EQUAL:
                return new GreaterThanOrEqualOperator(key, value);
            case LESS_THAN_OR_EQUAL:
                return new LessThanOrEqualOperator(key, value);
            case REGEX:
                return new RegexOperator(key, value);
        }
        return null;
    }

    @Override
    public boolean matches(JsonNode device) {
        final StringTokenizer tokenizer = new StringTokenizer(prepareKey(), ".");
        JsonNode jsonValue = device;
        while (tokenizer.hasMoreTokens() && jsonValue != null) {
            final String token = tokenizer.nextToken();
            jsonValue = jsonValue.get(token);
        }
        final Object value = OperatorDeserializer.parseValue(jsonValue);
        return compare(value);
    }

    String prepareKey() {
        for (int patternIdx = 0; patternIdx < patterns.length; patternIdx++) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(patterns[patternIdx]);
            Matcher matcher = pattern.matcher(getKey());
            if (matcher.matches()) {
                return matcher.replaceFirst(replacements[patternIdx]);
            }
        } // TODO: Duplicate code from OperatorConverter
        return getKey();
    }

    public abstract boolean compare(Object object);

    public abstract ComparisonOperatorType getType();
}