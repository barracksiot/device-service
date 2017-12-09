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

package io.barracks.deviceservice.repository;

import io.barracks.commons.test.MongoRepositoryTest;
import io.barracks.deviceservice.model.SegmentOrder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class SegmentOrderRepositoryTest extends MongoRepositoryTest {
    private SegmentOrderRepositoryImpl repository;
    private MongoTemplate mongoTemplate;

    public SegmentOrderRepositoryTest() {
        super(SegmentOrder.class.getDeclaredAnnotation(Document.class).collection());
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mongoTemplate = new MongoTemplate(getMongo(), getDatabaseName());
        repository = new SegmentOrderRepositoryImpl(mongoTemplate);
    }

    @Test
    public void updateOrder_whenNew_shouldCreateNew() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final SegmentOrder order = SegmentOrder
                .builder()
                .userId(userId)
                .segmentIds(new ArrayList<>(Collections.singletonList(UUID.randomUUID().toString())))
                .build();

        // When
        final SegmentOrder result = repository.updateOrder(userId, order.getSegmentIds());

        // Then
        assertThat(result).isEqualTo(order.toBuilder().id(result.getId()).build());
    }

    @Test
    public void updateOrder_whenExisting_shouldUpdate() {
        // Given
        final SegmentOrder order = SegmentOrder
                .builder()
                .userId(UUID.randomUUID().toString())
                .segmentIds(Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                .build();
        repository.updateOrder(order.getUserId(), new ArrayList<>(Collections.singletonList(UUID.randomUUID().toString())));

        // When
        final SegmentOrder result = repository.updateOrder(order.getUserId(), order.getSegmentIds());

        // Then
        assertThat(result).isEqualTo(order.toBuilder().id(result.getId()).build());
    }

    @Test
    public void findByUserId_ifNotSet_shouldReturnNewValidSegmentOrder() {
        // Given
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        final String userId = UUID.randomUUID().toString();

        // When
        final SegmentOrder result = repository.findByUserId(userId);
        final Set<ConstraintViolation<SegmentOrder>> violations = validator.validate(result);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(violations).isEmpty();
    }

    @Test
    public void findByUserId_ifSet_shouldReturnSavedSegmentOrder() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final SegmentOrder order = SegmentOrder.builder()
                .userId(userId)
                .segmentIds(Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                .build();
        mongoTemplate.save(order);

        // When
        final SegmentOrder result = repository.findByUserId(userId);

        // Then
        assertThat(result).isEqualTo(order);
    }
}
