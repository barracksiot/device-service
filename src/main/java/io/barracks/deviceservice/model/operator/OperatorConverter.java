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

import com.fasterxml.jackson.databind.util.ISO8601Utils;
import io.barracks.deviceservice.model.operator.comparison.ComparisonOperator;
import io.barracks.deviceservice.model.operator.logical.LogicalOperator;
import io.barracks.deviceservice.rest.exception.BarracksQueryFormatException;
import org.springframework.data.mongodb.core.query.Criteria;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class OperatorConverter {

    private static final String[] patterns = new String[]{
            "^(customClientData)(\\..+)?$",
            "^lastSeen$"
    };

    private static final String[] replacements = new String[]{
            "lastEvent.request.customClientData$2",
            "lastEvent.receptionDate"
    };

    static String getDatabaseKey(ComparisonOperator operator) {
        for (int patternIdx = 0; patternIdx < patterns.length; patternIdx++) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(patterns[patternIdx]);
            Matcher matcher = pattern.matcher(operator.getKey());
            if (matcher.matches()) {
                return matcher.replaceFirst(replacements[patternIdx]);
            }
        }
        return operator.getKey();
    }

    private static Object getDatabaseValue(ComparisonOperator operator) {
        return prepareValueForKey(operator.getKey(), operator.getValue());
    }

    private static Object prepareValueForKey(String key, Object value) {
        if (key.equals("lastSeen") || key.equals("firstSeen")) {
            try {
                return ISO8601Utils.parse((String) value, new ParsePosition(0));
            } catch (ParseException e) {
                throw new BarracksQueryFormatException(key + " - " + value, e);
            }
        }
        return value;
    }

    private static List<Object> getDatabaseValues(ComparisonOperator operator) {
        return ((List<Object>) operator.getValue()).stream()
                .map(obj -> prepareValueForKey(operator.getKey(), obj))
                .collect(Collectors.toList());
    }

    public static Criteria toMongoCriteria(Operator operator) {
        if (operator instanceof LogicalOperator) {
            List<Criteria> criteria = ((LogicalOperator) operator).getOperands()
                    .stream()
                    .map(OperatorConverter::toMongoCriteria)
                    .collect(Collectors.toList());
            Criteria criteriaArray[] = criteria.toArray(new Criteria[0]);
            Criteria criterion = new Criteria();
            switch (((LogicalOperator) operator).getType()) {
                case AND:
                    criterion.andOperator(criteriaArray);
                    break;
                case OR:
                    criterion.orOperator(criteriaArray);
                    break;
                default:
                    throw new IllegalArgumentException("Failed to parse the operator to a query");
            }
            return criterion;
        } else {
            ComparisonOperator comparisonOperator = (ComparisonOperator) operator;
            Criteria criteria = Criteria.where(getDatabaseKey(comparisonOperator));
            switch (comparisonOperator.getType()) {
                case EQUAL:
                    criteria.is(getDatabaseValue(comparisonOperator));
                    break;
                case NOT_EQUAL:
                    criteria.ne(getDatabaseValue(comparisonOperator));
                    break;
                case IN:
                    criteria.in(getDatabaseValues(comparisonOperator));
                    break;
                case NIN:
                    criteria.nin(getDatabaseValues(comparisonOperator));
                    break;
                case GREATER_THAN:
                    criteria.gt(getDatabaseValue(comparisonOperator));
                    break;
                case LESS_THAN:
                    criteria.lt(getDatabaseValue(comparisonOperator));
                    break;
                case GREATER_THAN_OR_EQUAL:
                    criteria.gte(getDatabaseValue(comparisonOperator));
                    break;
                case LESS_THAN_OR_EQUAL:
                    criteria.lte(getDatabaseValue(comparisonOperator));
                    break;
                case REGEX:
                    criteria.regex(java.util.regex.Pattern.compile((String) comparisonOperator.getValue())); // TODO WTF IF WE USE A DATE
                    break;
                default:
                    throw new IllegalArgumentException("Failed to parse the operator to a query");
            }
            return criteria;
        }
    }
}
