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

package io.barracks.deviceservice.manager;

import io.barracks.deviceservice.model.DataSet;
import io.barracks.deviceservice.repository.DeviceEventRepository;
import io.barracks.deviceservice.repository.DeviceRepository;
import io.barracks.deviceservice.utils.DataSetUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class StatsManagerTest {
    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private DeviceEventRepository deviceEventRepository;

    private StatsManager statsManager;

    @Before
    public void setUp() {
        statsManager = new StatsManager(deviceRepository, deviceEventRepository);
        reset(deviceRepository);
    }

    @Test
    public void getSeenDevices_shouldForwardCallToRepository() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final OffsetDateTime start = OffsetDateTime.MIN;
        final OffsetDateTime end = OffsetDateTime.MAX;
        final DataSet expected = DataSetUtils.getDataSet();
        doReturn(expected).when(deviceEventRepository).getSeenDeviceCount(userId, start, end);

        // When
        final DataSet result = statsManager.getSeenDeviceCount(userId, start, end);

        // Then
        verify(deviceEventRepository).getSeenDeviceCount(userId, start, end);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getLastSeenDevices_shouldForwardCallToRepository() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final OffsetDateTime start = OffsetDateTime.MIN;
        final OffsetDateTime end = OffsetDateTime.MAX;
        final DataSet expected = DataSetUtils.getDataSet();
        doReturn(expected).when(deviceRepository).getLastSeenDeviceCount(userId, start, end);

        // When
        final DataSet result = statsManager.getLastSeenDeviceCount(userId, start, end);

        // Then
        verify(deviceRepository).getLastSeenDeviceCount(userId, start, end);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getDeviceCountPerUserId_shouldForwardCallToRepository() {
        // Given
        final DataSet expected = DataSetUtils.getDataSet();
        doReturn(expected).when(deviceRepository).getDeviceCountPerUserId();

        // When
        final DataSet result = statsManager.getDeviceCountPerUserId();

        // Then
        verify(deviceRepository).getDeviceCountPerUserId();
        assertThat(result).isEqualTo(expected);
    }
}
