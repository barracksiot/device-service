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

package io.barracks.deviceservice.rest;

import io.barracks.deviceservice.manager.StatsManager;
import io.barracks.deviceservice.model.DataSet;
import io.barracks.deviceservice.rest.entity.DateRange;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.UUID;

import static io.barracks.deviceservice.utils.DataSetUtils.getDataSet;
import static io.barracks.deviceservice.utils.DateRangeUtils.getDateRange;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class StatsResourceTest {

    @Mock
    private StatsManager manager;

    @InjectMocks
    private StatsResource resource;

    @Test
    public void getAliveDeviceCount_shouldForwardCallToManager_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final DateRange range = getDateRange();
        final DataSet expected = getDataSet();
        doReturn(expected).when(manager).getLastSeenDeviceCount(userId, range.getStart(), range.getEnd());

        // When
        final DataSet result = resource.getAliveDevicesCount(userId, range);

        // Then
        verify(manager).getLastSeenDeviceCount(userId, range.getStart(), range.getEnd());
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getUniqueDevicesCount_shouldForwardCallToManager_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final DateRange range = getDateRange();
        final DataSet expected = getDataSet();
        doReturn(expected).when(manager).getSeenDeviceCount(userId, range.getStart(), range.getEnd());

        // When
        final DataSet result = resource.getUniqueDevicesCount(userId, range);

        // Then
        verify(manager).getSeenDeviceCount(userId, range.getStart(), range.getEnd());
        assertThat(result).isEqualTo(expected);

    }

    @Test
    public void getDeviceCountPerUserId_shouldForwardCallToManager_andReturnResult() {
        // Given
        final DataSet expected = getDataSet();
        doReturn(expected).when(manager).getDeviceCountPerUserId();

        // When
        final DataSet result = resource.getDeviceCountPerUserId();

        // Then
        verify(manager).getDeviceCountPerUserId();
        assertThat(result).isEqualTo(expected);
    }
}
