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
import com.mongodb.DBObject;
import io.barracks.commons.test.WebApplicationTest;
import io.barracks.deviceservice.Application;
import io.barracks.deviceservice.config.RepositoryRestConfig;
import io.barracks.deviceservice.model.Segment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration({Application.class, EmbeddedMongoAutoConfiguration.class, RepositoryRestConfig.class})
@WebIntegrationTest(randomPort = true)
public class SegmentRepositoryTest extends WebApplicationTest {
    @Autowired
    private SegmentRepository segmentRepository;
    @Autowired
    private MongoTemplate mongoTemplate;

    @Before
    public void setup() {
        for (String collection : mongoTemplate.getCollectionNames()) {
            if (!collection.startsWith("system.")) {
                mongoTemplate.remove(new Query(), collection);
            }
        }
    }

    @Test
    public void saveAndFindSegment_shouldReturnEqualValues() throws Exception {
        // Given
        final Segment segment = new ObjectMapper().readValue(getJsonFromResource("operator").toJSONString(), Segment.class);
        final Segment saved = segmentRepository.insert(segment.toBuilder().build());
        final Segment expected = segment.toBuilder()
                .id(saved.getId())
                .updated(saved.getUpdated())
                .build();

        // When
        final Segment result = segmentRepository.findOne(saved.getId());

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void saveSegment_shouldSaveOperatorAsString() throws Exception {
        // Given
        final Segment segment = new ObjectMapper().readValue(getJsonFromResource("operator").toJSONString(), Segment.class);
        segmentRepository.insert(segment);

        // When
        DBObject result = mongoTemplate.findOne(Query.query(where("_id").is(segment.getId())), DBObject.class, Segment.class.getAnnotation(Document.class).collection());

        // Then
        assertThat(result.get("query")).isInstanceOf(String.class);
    }

    @Test
    public void saveSegment_shouldUpdateDate() {
        // Given
        Segment segment = Segment.builder().userId(UUID.randomUUID().toString()).name(UUID.randomUUID().toString()).build();

        // When
        final Segment result = segmentRepository.insert(segment);

        // Then
        assertThat(result.getUpdated()).isNotNull();
    }

    @Test
    public void updateSegment_shouldUpdateDate() throws Exception {
        // Given
        Segment segment = Segment.builder().userId(UUID.randomUUID().toString()).name(UUID.randomUUID().toString()).build();
        segmentRepository.insert(segment);
        Date time = segment.getUpdated();
        Thread.sleep(100);

        // When
        final Segment result = segmentRepository.save(segment);

        // Then
        assertThat(result.getUpdated()).isNotEqualTo(time);
    }

    @Test
    public void getActiveSegments_whenOrderIsEmpty_shouldReturnNothing() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Segment segment = Segment.builder().userId(userId).name(UUID.randomUUID().toString()).build();
        segmentRepository.insert(segment);

        // When
        final List<Segment> result = segmentRepository.getSegmentsInIds(userId, Collections.emptyList());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void getActiveSegments_whenOrderIsNotEmpty_shouldReturnSegments() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Segment segment = Segment.builder().userId(userId).name(UUID.randomUUID().toString()).build();
        segmentRepository.insert(segment);

        // When
        final List<Segment> result = segmentRepository.getSegmentsInIds(userId, Collections.singletonList(segment.getId()));

        // Then
        assertThat(result).contains(segment);
    }

    @Test
    public void getInactiveSegments_whenOrderContainsIds_shouldNotReturnSegments() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Segment segment = Segment.builder().userId(userId).name(UUID.randomUUID().toString()).build();
        segmentRepository.insert(segment);

        // When
        final List<Segment> result = segmentRepository.getSegmentsNotInIds(userId, Collections.singletonList(segment.getId()));

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void getInactiveSegments_whenOrderIsEmpty_shouldReturnNothing() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Segment segment = Segment.builder().userId(userId).name(UUID.randomUUID().toString()).build();
        segmentRepository.insert(segment);

        // When
        final List<Segment> result = segmentRepository.getSegmentsNotInIds(userId, Collections.emptyList());

        // Then
        assertThat(result).contains(segment);
    }

}
