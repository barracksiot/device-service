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

public abstract class ComparableOperator extends ComparisonOperator {

    public ComparableOperator(String key, Object value) {
        super(key, value);
    }

    private int compareInternal(Object object) {
        if (getValue() == null || !(getValue() instanceof Comparable) || object == null || !(object instanceof Comparable)) {
            throw new ClassCastException();
        }
        return ((Comparable) object).compareTo(getValue());
    }

    protected abstract boolean computeResult(int result);

    @Override
    public boolean compare(Object object) {
        try {
            return computeResult(compareInternal(object));
        } catch (ClassCastException e) {
            return false;
        }
    }
}