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

import io.barracks.deviceservice.model.DataSet;
import io.barracks.deviceservice.model.Device;
import io.barracks.deviceservice.model.DeviceEvent;
import io.barracks.deviceservice.utils.DeviceEventUtils;
import io.barracks.deviceservice.utils.DeviceUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@BarracksRepositoryTest
public class DeviceEventRepositoryStatsTest {
    @Autowired
    private DeviceRepository deviceRepository;

    @Before
    public void setUp() throws Exception {
        deviceRepository.deleteAll();
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


    private void createRandomDeviceDataSet(String userId, OffsetDateTime start, OffsetDateTime end, boolean withEmptyEvents) {
        final List<String> deviceIds = getRandomIds(1000);
        final SecureRandom random = new SecureRandom();
        // 200 Before
        for (int deviceIdx = 0; deviceIdx < 200; deviceIdx++) {
            saveDevice(
                    userId,
                    deviceIds.get(deviceIdx),
                    OffsetDateTime.ofInstant(
                            start.toInstant().minus(1, MILLIS).minus(random.nextInt(1000), MILLIS),
                            start.getOffset()
                    )
            );
        }
        // 200 in
        for (int deviceIdx = 200; deviceIdx < 800; deviceIdx++) {
            saveDevice(
                    userId,
                    deviceIds.get(deviceIdx),
                    OffsetDateTime.ofInstant(
                            start.toInstant().plus(1, MILLIS).plus(random.nextInt((int) (end.toInstant().toEpochMilli() - start.toInstant().toEpochMilli() - 2)), MILLIS),
                            start.getOffset()
                    )
            );
        }
        // 200 after
        for (int deviceIdx = 800; deviceIdx < 1000; deviceIdx++) {
            saveDevice(
                    userId,
                    deviceIds.get(deviceIdx),
                    OffsetDateTime.ofInstant(
                            end.toInstant().plus(1, MILLIS).plus(random.nextInt(1000), MILLIS),
                            end.getOffset()
                    )
            );
        }
        if (withEmptyEvents) {
            for (int deviceIdx = 0; deviceIdx < 2000; deviceIdx++) {
                final String unitId = UUID.randomUUID().toString();
                final Device device = DeviceUtils.getDevice().toBuilder()
                        .lastEvent(null)
                        .userId(userId)
                        .unitId(unitId)
                        .build();
                deviceRepository.save(device);
            }
        }
    }

    private void saveDevice(String userId, String unitId, OffsetDateTime lastEventDate) {
        final DeviceEvent event = DeviceEventUtils.getDeviceEvent().toBuilder()
                .userId(userId)
                .unitId(unitId)
                .receptionDate(lastEventDate)
                .build();
        final Device device = DeviceUtils.getDevice().toBuilder()
                .userId(userId)
                .unitId(unitId)
                .lastEvent(event)
                .build();
        deviceRepository.save(device);
    }

    private List<String> getRandomIds(int count) {
        List<String> ids = new ArrayList<>();
        for (int versionIdx = 0; versionIdx < count; versionIdx++) {
            ids.add(UUID.randomUUID().toString());
        }
        return ids;
    }
}
