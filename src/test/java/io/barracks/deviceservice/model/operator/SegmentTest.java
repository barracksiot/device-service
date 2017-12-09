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

import io.barracks.deviceservice.model.Segment;
import io.barracks.deviceservice.model.operator.comparison.ComparisonOperator;
import org.junit.Before;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class SegmentTest {
    private Validator validator;
    private Segment validSegment;

    @Before
    public void init() {
        ValidatorFactory vf = Validation.buildDefaultValidatorFactory();
        this.validator = vf.getValidator();
        this.validSegment = Segment.builder()
                .id(UUID.randomUUID().toString())
                .name(UUID.randomUUID().toString().replace("-", ""))
                .query(ComparisonOperator.from("eq", "unitId", UUID.randomUUID().toString()))
                .updated(new Date())
                .userId(UUID.randomUUID().toString())
                .build();
    }

    @Test
    public void ensureValidSegmentIsValid() {
        // Given

        // When
        final Set<ConstraintViolation<Segment>> violations = this.validator.validate(validSegment);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    public void ensureSegmentNameNotNull() {
        // Given
        final Segment segment = validSegment.toBuilder().name(null).build();

        // When
        final Set<ConstraintViolation<Segment>> violations = this.validator.validate(segment);

        // Then
        assertFalse(violations.isEmpty());
    }

    @Test
    public void ensureSegmentNameNotEmpty() {
        // Given
        final Segment segment = validSegment.toBuilder().name("").build();

        // When
        final Set<ConstraintViolation<Segment>> violations = this.validator.validate(segment);

        // Then
        assertFalse(violations.isEmpty());
    }

    @Test
    public void ensure1Char1NameOK() {
        // Given
        final Segment segment = validSegment.toBuilder().name("t").build();

        // When
        final Set<ConstraintViolation<Segment>> violations = this.validator.validate(segment);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    public void ensureSegmentNameCorrectChars() {
        // Given
        final Segment segment = validSegment.toBuilder().name("test with spaces").build();

        // When
        final Set<ConstraintViolation<Segment>> violations = this.validator.validate(segment);

        // Then
        assertFalse(violations.isEmpty());
    }

    @Test
    public void ensureSegmentNameNotTooLong() {
        // Given
        final Segment segment = validSegment.toBuilder().name("testWithAVeryLongNameWhichWeHopeShouldBeLongerThan50Characters").build();

        // When
        final Set<ConstraintViolation<Segment>> violations = this.validator.validate(segment);

        // Then
        assertFalse(violations.isEmpty());
    }

    @Test
    public void ensure50CharsNameOK() {
        // Given
        final Segment segment = validSegment.toBuilder().name("testWith50CharactersWhichShouldPasssssssssssssssss").build();

        // When
        final Set<ConstraintViolation<Segment>> violations = this.validator.validate(segment);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    public void ensure51CharsNameFail() {
        // Given
        final Segment segment = validSegment.toBuilder().name("testWith51CharactersWhichShouldNotPasssssssssssssss").build();

        // When
        final Set<ConstraintViolation<Segment>> violations = this.validator.validate(segment);

        // Then
        assertFalse(violations.isEmpty());
    }

    @Test
    public void ensureSegmentNameNotReserved() {
        // Given
        final Segment segment = validSegment.toBuilder().name("other").build();

        // When
        final Set<ConstraintViolation<Segment>> violations = this.validator.validate(segment);

        // Then
        assertFalse(violations.isEmpty());
    }

    @Test
    public void ensureSegmentNameOk() {
        // Given
        final Segment segment = validSegment.toBuilder().name("otherSegmentName").build();

        // When
        final Set<ConstraintViolation<Segment>> violations = this.validator.validate(segment);

        // Then
        assertTrue(violations.isEmpty());
    }
}
