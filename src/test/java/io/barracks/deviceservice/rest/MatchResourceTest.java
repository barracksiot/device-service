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

import io.barracks.deviceservice.manager.FilterManager;
import io.barracks.deviceservice.model.DeviceComponentRequest;
import io.barracks.deviceservice.model.Filter;
import io.barracks.deviceservice.utils.DeviceComponentRequestUtils;
import io.barracks.deviceservice.utils.FilterUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class MatchResourceTest {
    @Mock
    private FilterManager manager;

    @InjectMocks
    private MatchResource resource;

    @Test
    public void matchEvent_whenFirstTrue_shouldCallFindFirstMatchingFilter() {
        // Given
        final List<String> names = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        final Filter expected = FilterUtils.getFilter();
        final DeviceComponentRequest event = DeviceComponentRequestUtils.getDeviceComponentRequest();
        doReturn(expected).when(manager).findFirstMatchingFilter(names, event);

        // When
        final Object result = resource.matchEvent(names, true, event);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void matchEvent_whenFirstFalse_shouldCallFindMatchingFilters() {
        // Given
        final List<String> names = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        final List<Filter> expected = Arrays.asList(FilterUtils.getFilter(), FilterUtils.getFilter());
        final DeviceComponentRequest event = DeviceComponentRequestUtils.getDeviceComponentRequest();
        doReturn(expected).when(manager).findMatchingFilters(names, event);

        // When
        final Object result = resource.matchEvent(names, false, event);

        // Then
        assertThat(result).isEqualTo(expected);
    }
}
