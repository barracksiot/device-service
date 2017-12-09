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
import com.fasterxml.jackson.databind.util.ISO8601Utils;
import com.google.common.collect.ImmutableMap;
import io.barracks.commons.test.WebApplicationTest;
import io.barracks.deviceservice.Application;
import io.barracks.deviceservice.config.RepositoryRestConfig;
import io.barracks.deviceservice.model.Device;
import io.barracks.deviceservice.model.DeviceConfiguration;
import io.barracks.deviceservice.model.DeviceEvent;
import io.barracks.deviceservice.model.operator.Operator;
import io.barracks.deviceservice.model.operator.OperatorConverter;
import io.barracks.deviceservice.model.operator.comparison.EqualOperator;
import io.barracks.deviceservice.model.operator.logical.AndOperator;
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

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration({Application.class, EmbeddedMongoAutoConfiguration.class, RepositoryRestConfig.class})
@WebIntegrationTest(randomPort = true)
public class DeviceRepositoryTest extends WebApplicationTest {
    private static final String collection = Device.class.getAnnotation(Document.class).collection();

    @Autowired
    private DeviceRepository deviceRepository;
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
    public void saveAndFindDevice_shouldSaveLastEventAsEvent() throws Exception {
        // Given
        final String userId = "donaldDrumpf";
        final String unitId = "MyGreatUnit";
        final Device device = Device.builder()
                .userId(userId)
                .unitId(unitId)
                .firstSeen(new Date())
                .lastEvent(DeviceEvent.builder()
                        .id("plop")
                        .unitId(unitId)
                        .userId(userId)
                        .versionId("v2")
                        .receptionDate(new Date(12345678L))
                        .build()
                )
                .configuration(DeviceConfiguration.builder()
                        .id("replop")
                        .unitId(unitId)
                        .userId(userId)
                        .build()
                )
                .build();
        mongoTemplate.insert(device.toBuilder().build());

        // When
        String result = mongoTemplate.findOne(new Query(), String.class, collection);

        // Then
        assertTrue(new ObjectMapper().readTree(result).has("event"));
    }

    @Test
    public void lookupDeviceUsingCriteria_shouldFindDevice() {
        // Given
        final Date date = new Date(1234567890L);
        final String key = "key";
        final String value = "value";
        final String versionId = UUID.randomUUID().toString();
        final Operator matcher = new AndOperator(
                Arrays.asList(
                        new EqualOperator("versionId", versionId),
                        new EqualOperator("lastSeen", ISO8601Utils.format(date, true)),
                        new EqualOperator("customClientData." + key, value)
                )
        );

        final Device device = Device.builder().lastEvent(
                DeviceEvent.builder()
                        .versionId(versionId)
                        .receptionDate(date)
                        .additionalProperties(ImmutableMap.of(key, value))
                        .build()
        ).build();
        final Device expected = deviceRepository.insert(device);

        // When
        final Device result = mongoTemplate.findOne(Query.query(OperatorConverter.toMongoCriteria(matcher)), Device.class, collection);

        // Then
        assertThat(result).isEqualTo(expected);
    }
}
