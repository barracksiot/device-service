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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.commons.test.JsonResourceLoader;
import io.barracks.commons.test.MongoRepositoryTest;
import io.barracks.deviceservice.model.operator.Operator;
import io.barracks.deviceservice.model.Segment;
import io.barracks.deviceservice.rest.SegmentResourceTest;
import net.minidev.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

public class SegmentRepositoryTest extends MongoRepositoryTest {
    private MongoTemplate mongoTemplate;
    private SegmentRepositoryImpl segmentRepository;

    public SegmentRepositoryTest() {
        super(Segment.class.getDeclaredAnnotation(Document.class).collection());
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mongoTemplate = new MongoTemplate(getMongo(), getDatabaseName());
        segmentRepository = spy(new SegmentRepositoryImpl(mongoTemplate));
    }

    @Test
    public void saveAndFind_shouldReturnEqualPojo() throws Exception {
        // Given
        final JSONObject json = JsonResourceLoader.getJsonFromResource(SegmentResourceTest.class, "request");
        final Segment segment = Segment.builder()
                .userId(json.getAsString("userId"))
                .name(json.getAsString("name"))
                .query(new ObjectMapper().readValue(((JSONObject) json.get("query")).toJSONString(), Operator.class))
                .build();

        // When
        mongoTemplate.insert(segment);
        Segment result = mongoTemplate.findById(segment.getId(), Segment.class);

        // Then
        assertThat(result).isEqualTo(segment);
    }

    @Test
    public void save_whenUsingTheSameNameTwice_shouldFail() {
        // Given
        final String name = UUID.randomUUID().toString();
        final Segment s1 = Segment.builder().name(name).build();
        final Segment s2 = Segment.builder().name(name).build();
        mongoTemplate.insert(s1);

        // Then When
        assertThatExceptionOfType(DuplicateKeyException.class).isThrownBy(() -> mongoTemplate.insert(s2));
    }

    @Test
    public void getSegmentInIds_shouldCallGetSegmentByIds_andReturnResults() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final List<String> ids = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        final List<Segment> segments = Collections.singletonList(Segment.builder().id(UUID.randomUUID().toString()).build());
        doReturn(segments).when(segmentRepository).getSegmentsByIds(userId, ids, true);

        // When
        final List<Segment> results = segmentRepository.getSegmentsInIds(userId, ids);

        // Then
        verify(segmentRepository).getSegmentsByIds(userId, ids, true);
        assertThat(results).isEqualTo(segments);
    }

    @Test
    public void getSegmentNotInIds_shouldCallGetSegmentByIds_andReturnResults() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final List<String> ids = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        final List<Segment> segments = Collections.singletonList(Segment.builder().id(UUID.randomUUID().toString()).build());
        doReturn(segments).when(segmentRepository).getSegmentsByIds(userId, ids, false);

        // When
        final List<Segment> results = segmentRepository.getSegmentsNotInIds(userId, ids);

        // Then
        verify(segmentRepository).getSegmentsByIds(userId, ids, false);
        assertThat(results).isEqualTo(segments);
    }

    @Test
    public void getSegmentsById_shouldMatchIds_whenMatchIds() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Segment segment = Segment.builder().userId(userId).build();
        mongoTemplate.save(segment);
        final List<String> segmentIds = Collections.singletonList(segment.getId());

        // When
        final List<Segment> result = segmentRepository.getSegmentsByIds(userId, segmentIds, true);

        // Then
        assertThat(result).containsExactly(segment);
    }

    @Test
    public void getSegmentsById_shouldIgnoreOtherIds_whenMatchIds() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Segment segment = Segment.builder().userId(userId).build();
        mongoTemplate.save(segment);
        final List<String> segmentIds = Collections.singletonList(UUID.randomUUID().toString());

        // When
        final List<Segment> result = segmentRepository.getSegmentsByIds(userId, segmentIds, true);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void getSegmentsById_shouldNotMatchIds_whenNotMatchIds() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Segment segment = Segment.builder().userId(userId).build();
        mongoTemplate.save(segment);
        final List<String> segmentIds = Collections.singletonList(segment.getId());

        // When
        final List<Segment> result = segmentRepository.getSegmentsByIds(userId, segmentIds, false);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void getSegmentsById_shouldIncludeOtherIds_whenNotMatchIds() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Segment segment = Segment.builder().userId(userId).build();
        mongoTemplate.save(segment);
        final List<String> segmentIds = Collections.singletonList(UUID.randomUUID().toString());

        // When
        final List<Segment> result = segmentRepository.getSegmentsByIds(userId, segmentIds, false);

        // Then
        assertThat(result).containsExactly(segment);
    }

    @Test
    public void getSegmentsById_ignoreOtherUserIds_whenNotMatchIds() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Segment segment = Segment.builder().userId(UUID.randomUUID().toString()).build();
        mongoTemplate.save(segment);
        final List<String> segmentIds = Collections.singletonList(UUID.randomUUID().toString());

        // When
        final List<Segment> result = segmentRepository.getSegmentsByIds(userId, segmentIds, false);

        // Then
        assertThat(result).isEmpty();
    }
}
