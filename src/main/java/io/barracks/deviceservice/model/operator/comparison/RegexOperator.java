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

import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;

public class RegexOperator extends ComparisonOperator {

    public RegexOperator(String key, Object value) {
        super(key, value);
    }

    @Override
    public boolean compare(Object object) {
        if (object == null || getValue() == null || !(getValue() instanceof String) || !(object instanceof CharSequence)) {
            return false;
        }
        Matcher matcher;
        try {
            matcher = java.util.regex.Pattern.compile((String) getValue()).matcher((CharSequence) object);
        } catch (PatternSyntaxException pse) {
            return false;
        }
        return matcher.matches();
    }

    @Override
    public ComparisonOperatorType getType() {
        return ComparisonOperatorType.REGEX;
    }
}