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

package io.barracks.deviceservice.model.operator.validation;

import io.barracks.deviceservice.model.Filter;
import org.junit.Before;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static io.barracks.deviceservice.utils.FilterUtils.getFilter;
import static org.assertj.core.api.Assertions.assertThat;

public class FilterValidationTest {
    private Validator validator;
    private Filter validFilter;

    @Before
    public void init() {
        ValidatorFactory vf = Validation.buildDefaultValidatorFactory();
        this.validator = vf.getValidator();
        this.validFilter = getFilter();
    }

    @Test
    public void ensureValidFilterIsValid() {
        // Given

        // When
        final Set<ConstraintViolation<Filter>> violations = this.validator.validate(validFilter);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    public void ensureFilterNameNotNull() {
        // Given
        final Filter filter = validFilter.toBuilder().name(null).build();

        // When
        final Set<ConstraintViolation<Filter>> violations = this.validator.validate(filter);

        // Then
        assertThat(violations).isNotEmpty();
    }

    @Test
    public void ensureFilterNameNotEmpty() {
        // Given
        final Filter filter = validFilter.toBuilder().name("").build();

        // When
        final Set<ConstraintViolation<Filter>> violations = this.validator.validate(filter);

        // Then
        assertThat(violations).isNotEmpty();
    }

    @Test
    public void ensure1CharNameOK() {
        // Given
        final Filter filter = validFilter.toBuilder().name("t").build();

        // When
        final Set<ConstraintViolation<Filter>> violations = this.validator.validate(filter);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    public void ensureFilterNameOk() {
        // Given
        final Filter filter = validFilter.toBuilder().name("otherFilterName").build();

        // When
        final Set<ConstraintViolation<Filter>> violations = this.validator.validate(filter);

        // Then
        assertThat(violations).isEmpty();
    }
}
