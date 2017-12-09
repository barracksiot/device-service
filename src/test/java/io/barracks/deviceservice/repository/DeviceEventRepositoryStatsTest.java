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
import io.barracks.deviceservice.model.DataSet;
import io.barracks.deviceservice.model.DeviceEvent;
import io.barracks.deviceservice.model.Device;
import io.barracks.deviceservice.model.DeviceConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

@RunWith(MockitoJUnitRunner.class)
public class DeviceEventRepositoryStatsTest extends MongoRepositoryTest {
    private DeviceRepositoryImpl deviceRepository;
    private MongoTemplate mongoTemplate;

    public DeviceEventRepositoryStatsTest() {
        super(Device.class.getDeclaredAnnotation(Document.class).collection());
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mongoTemplate = new MongoTemplate(getMongo(), getDatabaseName());
        deviceRepository = spy(new DeviceRepositoryImpl(mongoTemplate));
    }

    @Test
    public void getDeviceCountPerVersionId_shouldReflectDataInDb() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        final DataSet dataSet = createRandomDevicePerVersionIdDataSet(userId, segmentId, false);

        // When
        DataSet result = deviceRepository.getDevicesCountPerVersionId(userId);

        // Then
        assertThat(result).isEqualTo(dataSet);
    }

    @Test
    public void getDeviceCountPerVersionId_shouldIgnoreDevicesWithNoEvent() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        final DataSet dataSet = createRandomDevicePerVersionIdDataSet(userId, segmentId, true);

        // When
        DataSet result = deviceRepository.getDevicesCountPerVersionId(userId);

        // Then
        assertThat(result).isEqualTo(dataSet);
    }

    @Test
    public void getDeviceCountPerVersion_whenNoSegmentId_shouldGetThemAll() {
        // Given
        final String userId = UUID.randomUUID().toString();
        DataSet expected = createRandomDevicePerVersionIdDataSet(userId, getRandomIds(5), getRandomIds(10), true);

        // When
        DataSet result = deviceRepository.getDevicesCountPerVersionId(userId);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getLastSeenDeviceCount_shouldReflectDataWithinBoundaries() {
        // Given
        final OffsetDateTime start = new Date(123456789000L).toInstant().atOffset(ZoneOffset.UTC);
        final OffsetDateTime end = new Date(123456790000L).toInstant().atOffset(ZoneOffset.UTC);
        final String userId = UUID.randomUUID().toString();
        final DataSet expected = DataSet.builder().total(BigDecimal.valueOf(600)).build();
        createRandomDeviceDataSet(userId, start, end, false);

        // When
        DataSet result = deviceRepository.getLastSeenDeviceCount(userId, start, end);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getLastSeenDeviceCount_shouldReflectDataBefore() {
        // Given
        final OffsetDateTime start = new Date(123456789000L).toInstant().atOffset(ZoneOffset.UTC);
        final OffsetDateTime end = new Date(123456790000L).toInstant().atOffset(ZoneOffset.UTC);
        final String userId = UUID.randomUUID().toString();
        final DataSet expected = DataSet.builder().total(BigDecimal.valueOf(200)).build();
        createRandomDeviceDataSet(userId, start, end, false);

        // When
        DataSet result = deviceRepository.getLastSeenDeviceCount(userId, OffsetDateTime.MIN, start);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getLastSeenDeviceCount_shouldReflectDataBeforeAndDuring() {
        // Given
        final OffsetDateTime start = new Date(123456789000L).toInstant().atOffset(ZoneOffset.UTC);
        final OffsetDateTime end = new Date(123456790000L).toInstant().atOffset(ZoneOffset.UTC);
        final String userId = UUID.randomUUID().toString();
        final DataSet expected = DataSet.builder().total(BigDecimal.valueOf(800)).build();
        createRandomDeviceDataSet(userId, start, end, false);

        // When
        DataSet result = deviceRepository.getLastSeenDeviceCount(userId, OffsetDateTime.MIN, end);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getLastSeenDeviceCount_shouldReflectDataDuringAndAfter() {
        // Given
        final OffsetDateTime start = new Date(123456789000L).toInstant().atOffset(ZoneOffset.UTC);
        final OffsetDateTime end = new Date(123456790000L).toInstant().atOffset(ZoneOffset.UTC);
        final String userId = UUID.randomUUID().toString();
        final DataSet expected = DataSet.builder().total(BigDecimal.valueOf(800)).build();
        createRandomDeviceDataSet(userId, start, end, false);

        // When
        DataSet result = deviceRepository.getLastSeenDeviceCount(userId, start, OffsetDateTime.MAX);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getLastSeenDeviceCount_shouldReflectDataAfter() {
        // Given
        final OffsetDateTime start = new Date(123456789000L).toInstant().atOffset(ZoneOffset.UTC);
        final OffsetDateTime end = new Date(123456790000L).toInstant().atOffset(ZoneOffset.UTC);
        final String userId = UUID.randomUUID().toString();
        final DataSet expected = DataSet.builder().total(BigDecimal.valueOf(200)).build();
        createRandomDeviceDataSet(userId, start, end, false);

        // When
        DataSet result = deviceRepository.getLastSeenDeviceCount(userId, end, OffsetDateTime.MAX);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getLastSeenDeviceCount_shouldIgnoreEmptyEvents() {
        // Given
        final OffsetDateTime start = new Date(123456789000L).toInstant().atOffset(ZoneOffset.UTC);
        final OffsetDateTime end = new Date(123456790000L).toInstant().atOffset(ZoneOffset.UTC);
        final String userId = UUID.randomUUID().toString();
        final DataSet expected = DataSet.builder().total(BigDecimal.valueOf(600)).build();
        createRandomDeviceDataSet(userId, start, end, true);

        // When
        DataSet result = deviceRepository.getLastSeenDeviceCount(userId, start, end);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    private DataSet createRandomDevicePerVersionIdDataSet(String userId, String segmentId, boolean withEmptyEvent) {
        List<String> versionIds = getRandomIds(10);
        return createRandomDevicePerVersionIdDataSet(userId, segmentId, versionIds, withEmptyEvent);
    }

    private DataSet createRandomDevicePerVersionIdDataSet(String userId, List<String> segmentIds, List<String> versionIds, boolean withEmptyEvent) {
        DataSet result = DataSet.builder().total(BigDecimal.ZERO).build();
        for (String segmentId : segmentIds) {
            DataSet created = createRandomDevicePerVersionIdDataSet(userId, segmentId, versionIds, withEmptyEvent);
            DataSet.Builder builder = DataSet.builder();
            builder.total(result.getTotal().add(created.getTotal()));
            for (String versionId : versionIds) {
                BigDecimal old = result.getValues().containsKey(versionId) ? result.getValues().get(versionId) : BigDecimal.ZERO;
                BigDecimal recent = created.getValues().containsKey(versionId) ? created.getValues().get(versionId) : BigDecimal.ZERO;
                builder.value(versionId, old.add(recent));
            }
            result = builder.build();
        }
        return result;
    }

    private DataSet createRandomDevicePerVersionIdDataSet(String userId, String segmentId, List<String> versionIds, boolean withEmptyEvent) {
        final SecureRandom random = new SecureRandom();
        final DataSet.Builder builder = DataSet.builder();
        long total = 0;
        for (String versionId : versionIds) {
            int count = random.nextInt(20);
            if (count != 0) {
                builder.value(versionId, BigDecimal.valueOf(count));
                total += count;
                for (int unitIdx = 0; unitIdx < count; unitIdx++) {
                    String unitId = UUID.randomUUID().toString();
                    DeviceConfiguration configuration = DeviceConfiguration.builder()
                            .userId(userId)
                            .unitId(unitId)
                            .build();
                    DeviceEvent event = DeviceEvent.builder()
                            .userId(userId)
                            .unitId(unitId)
                            .versionId(versionId)
                            .segmentId(segmentId)
                            .build();
                    mongoTemplate.insert(Device.builder().userId(userId).unitId(unitId).lastEvent(event).configuration(configuration).build());
                }
                if (withEmptyEvent) {
                    int noEventCount = 20 + random.nextInt(20);
                    for (int unitIdx = 0; unitIdx < noEventCount; unitIdx++) {
                        String unitId = UUID.randomUUID().toString();
                        DeviceConfiguration configuration = DeviceConfiguration.builder()
                                .userId(userId)
                                .unitId(unitId)
                                .build();
                        mongoTemplate.insert(Device.builder().userId(userId).unitId(unitId).configuration(configuration).build());
                    }
                }
            }
        }
        return builder.total(BigDecimal.valueOf(total)).build();
    }


    private void createRandomDeviceDataSet(String userId, OffsetDateTime start, OffsetDateTime end, boolean withEmptyEvents) {
        final List<String> deviceIds = getRandomIds(1000);
        final SecureRandom random = new SecureRandom();
        final String segmentId = UUID.randomUUID().toString();
        // 200 Before
        for (int deviceIdx = 0; deviceIdx < 200; deviceIdx++) {
            insertDeviceConfiguration(
                    userId,
                    deviceIds.get(deviceIdx),
                    segmentId,
                    new Date(start.toInstant().toEpochMilli() - 1 - random.nextInt(1000))
            );
        }
        // 200 in
        for (int deviceIdx = 200; deviceIdx < 800; deviceIdx++) {
            insertDeviceConfiguration(
                    userId,
                    deviceIds.get(deviceIdx),
                    segmentId,
                    new Date(start.toInstant().toEpochMilli() + 1 + random.nextInt((int) (end.toInstant().toEpochMilli() - start.toInstant().toEpochMilli() - 2)))
            );
        }
        // 200 after
        for (int deviceIdx = 800; deviceIdx < 1000; deviceIdx++) {
            insertDeviceConfiguration(
                    userId,
                    deviceIds.get(deviceIdx),
                    segmentId,
                    new Date(end.toInstant().toEpochMilli() + 1 + random.nextInt(1000))
            );
        }
        if (withEmptyEvents) {
            for (int deviceIdx = 0; deviceIdx < 2000; deviceIdx++) {
                String unitId = UUID.randomUUID().toString();
                DeviceConfiguration configuration = DeviceConfiguration.builder()
                        .userId(userId).unitId(unitId).build();
                mongoTemplate.insert(
                        Device.builder().userId(userId).unitId(unitId)
                                .configuration(configuration).build());
            }
        }
    }

    private List<String> getRandomIds(int count) {
        List<String> ids = new ArrayList<>();
        for (int versionIdx = 0; versionIdx < count; versionIdx++) {
            ids.add(UUID.randomUUID().toString());
        }
        return ids;
    }

    private void insertDeviceConfiguration(String userId, String unitId, String segmentId, Date receptionDate) {
        DeviceEvent event = DeviceEvent.builder()
                .userId(userId)
                .unitId(unitId)
                .segmentId(segmentId)
                .receptionDate(receptionDate)
                .build();
        DeviceConfiguration configuration = DeviceConfiguration.builder()
                .userId(userId).unitId(unitId).build();
        mongoTemplate.insert(
                Device.builder().userId(userId).unitId(unitId)
                        .lastEvent(event).configuration(configuration).build());
    }
}
