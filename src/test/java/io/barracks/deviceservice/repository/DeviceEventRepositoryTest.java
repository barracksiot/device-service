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
import io.barracks.deviceservice.model.DeviceEvent;
import io.barracks.deviceservice.model.DeviceRequest;
import io.barracks.deviceservice.utils.DeviceEventUtils;
import io.barracks.deviceservice.utils.DeviceRequestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@BarracksRepositoryTest
public class DeviceEventRepositoryTest {

    @Autowired
    private DeviceEventRepository deviceRepository;

    @Before
    public void setUp() throws Exception {
        deviceRepository.deleteAll();
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

    @Test
    public void getSeenDeviceCount_shouldReturnCorrectDataSet() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final OffsetDateTime start = OffsetDateTime.now().minusDays(1);
        final OffsetDateTime end = OffsetDateTime.now().plusDays(1);
        generateRandomEvents(userId, start, end);

        // When
        final DataSet before = deviceRepository.getSeenDeviceCount(userId, OffsetDateTime.MIN, start);
        final DataSet in = deviceRepository.getSeenDeviceCount(userId, start, end);
        final DataSet after = deviceRepository.getSeenDeviceCount(userId, end, OffsetDateTime.MAX);

        // Then
        assertThat(before.getTotal()).isEqualTo(BigDecimal.valueOf(1));
        assertThat(in.getTotal()).isEqualTo(BigDecimal.valueOf(2));
        assertThat(after.getTotal()).isEqualTo(BigDecimal.valueOf(1));
    }

    @Test
    public void saveDeviceEvent_whenCustomClientDataWithDots_shouldReplaceThem() {
        //Given
        final boolean onlyChanged = true;
        final Pageable pageable = new PageRequest(0, 20);
        final String userId = UUID.randomUUID().toString();
        final String unit1 = UUID.randomUUID().toString();
        final DeviceEvent deviceEvent = DeviceEventUtils.getDeviceEvent(userId, unit1)
                .toBuilder()
                .request(
                        DeviceRequestUtils.getDeviceRequest()
                                .toBuilder()
                                .addCustomClientData("data.with.dots", "value.with.dots")
                                .build()
                )
                .build();

        //When
        deviceRepository.save(deviceEvent);
        final DeviceEvent result = deviceRepository.findByUserIdAndUnitId(userId, unit1, onlyChanged, pageable).getContent().get(0);

        //Then
        assertThat(result).isEqualTo(deviceEvent);
    }


    private List<DeviceEvent> createEvents(String userId, String unitId, boolean onlyChanged, Pageable pageable) {
        return IntStream.range(0, pageable.getPageSize())
                .mapToObj(index -> {
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            DeviceEvent event = DeviceEventUtils.getDeviceEvent();
                            return event.toBuilder()
                                    .userId(userId)
                                    .unitId(unitId)
                                    .changed(index % 2 == 0)
                                    .build();
                        }
                )
                .map(event -> deviceRepository.insert(event))
                .filter(event -> !onlyChanged || event.isChanged())
                .collect(Collectors.toList());
    }

    private void generateRandomEvents(String userId, OffsetDateTime start, OffsetDateTime end) {
        final String unit1 = UUID.randomUUID().toString();
        final String unit2 = UUID.randomUUID().toString();

        IntStream.range(1, 100)
                .mapToObj(val -> DeviceEventUtils.getDeviceEvent(userId, unit1).toBuilder().receptionDate(start.minusSeconds(val)).build())
                .forEach(event -> deviceRepository.save(event));
        IntStream.range(1, 100)
                .mapToObj(val -> DeviceEventUtils.getDeviceEvent(userId, unit1).toBuilder().receptionDate(start.plusSeconds(val)).build())
                .forEach(event -> deviceRepository.save(event));

        IntStream.range(1, 100)
                .mapToObj(val -> DeviceEventUtils.getDeviceEvent(userId, unit2).toBuilder().receptionDate(start.plusSeconds(val)).build())
                .forEach(event -> deviceRepository.save(event));
        IntStream.range(1, 100)
                .mapToObj(val -> DeviceEventUtils.getDeviceEvent(userId, unit2).toBuilder().receptionDate(end.plusSeconds(val)).build())
                .forEach(event -> deviceRepository.save(event));
    }

}
