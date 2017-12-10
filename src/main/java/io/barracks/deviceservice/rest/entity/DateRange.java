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

package io.barracks.deviceservice.rest.entity;

import cz.jirutka.validator.spring.SpELAssert;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@SpELAssert(value = "checkDates()", message = "start before end")
@EqualsAndHashCode
@ToString
public class DateRange {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime start;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime end;

    public OffsetDateTime getStart() {
        return start != null ? OffsetDateTime.ofInstant(start.toInstant(), ZoneOffset.UTC) : null;
    }

    public void setStart(OffsetDateTime start) {
        this.start = OffsetDateTime.ofInstant(start.toInstant(), ZoneOffset.UTC);
    }

    public OffsetDateTime getEnd() {
        return end != null ? OffsetDateTime.ofInstant(end.toInstant(), ZoneOffset.UTC) : null;
    }

    public void setEnd(OffsetDateTime end) {
        this.end = OffsetDateTime.ofInstant(end.toInstant(), ZoneOffset.UTC);
    }

    public boolean checkDates() {
        if (start == null) {
            start = OffsetDateTime.MIN;
        }
        if (end == null) {
            end = OffsetDateTime.MAX;
        }
        return start.isBefore(end);
    }
}