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
import io.barracks.deviceservice.model.DeviceEvent;
import io.barracks.deviceservice.model.DeviceEventDocument;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class DeviceEventRepositoryTest extends MongoRepositoryTest {
    private DeviceEventRepositoryImpl deviceRepository;
    private MongoTemplate mongoTemplate;

    public DeviceEventRepositoryTest() {
        super(DeviceEventDocument.class.getDeclaredAnnotation(Document.class).collection());
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mongoTemplate = new MongoTemplate(getMongo(), getDatabaseName());
        deviceRepository = new DeviceEventRepositoryImpl(mongoTemplate);
    }

    @Test
    public void findByUserIdAndUnitId_whenOnlyChanged_shouldReturnOnlyChanged() {
        // Given
        final boolean onlyChanged = true;
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 20);
        final List<DeviceEvent> expected = createEvents(userId, unitId, onlyChanged, pageable);

        // When
        Page<DeviceEvent> result = deviceRepository.findByUserIdAndUnitId(userId, unitId, onlyChanged, pageable);

        // Then
        assertThat(result.getContent()).containsExactlyElementsOf(expected);
    }

    @Test
    public void findByUserIdAndUnitId_whenNotOnlyChanged_shouldReturnAll() {
        // Given
        final boolean onlyChanged = false;
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 20);
        final List<DeviceEvent> expected = createEvents(userId, unitId, onlyChanged, pageable);

        // When
        Page<DeviceEvent> result = deviceRepository.findByUserIdAndUnitId(userId, unitId, onlyChanged, pageable);

        // Then
        assertThat(result.getContent()).containsExactlyElementsOf(expected);
    }

    private List<DeviceEvent> createEvents(String userId, String unitId, boolean onlyChanged, Pageable pageable) {
        ArrayList<DeviceEvent> events = new ArrayList<>(pageable.getPageSize());
        for (int eventIdx = 0; eventIdx < pageable.getPageSize(); eventIdx++) {
            DeviceEvent event = DeviceEvent.builder()
                    .userId(userId)
                    .unitId(unitId)
                    .versionId(UUID.randomUUID().toString())
                    .receptionDate(new Date())
                    .segmentId(UUID.randomUUID().toString())
                    .changed(eventIdx % 2 == 0)
                    .build();
            mongoTemplate.insert(event);
            if (!onlyChanged || event.isChanged()) {
                events.add(event);
            }
        }
        return events;
    }
}
