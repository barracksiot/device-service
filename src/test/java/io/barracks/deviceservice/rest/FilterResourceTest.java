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
import io.barracks.deviceservice.model.Filter;
import io.barracks.deviceservice.utils.FilterUtils;
import io.barracks.deviceservice.utils.PagedResourcesUtils;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.filter;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FilterResourceTest {
    @Mock
    private FilterManager filterManager;

    @Mock
    private PagedResourcesAssembler<Filter> filterPagedResourcesAssembler;

    @InjectMocks
    private FilterResource filterResource;

    @Test
    public void createFilter_withValidFilter_shouldCallManagerAndReturnValue() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Filter filter = FilterUtils.getFilter();
        final Filter expected = FilterUtils.getFilter();

        doReturn(expected).when(filterManager).createFilter(userId, filter);

        // When
        final Filter result = filterResource.createFilter(userId, filter);

        // Then
        verify(filterManager).createFilter(userId, filter);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getFilters_whenAllIsFine_shouldCallManagerAndReturnFilterList() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Filter filter1 = FilterUtils.getFilter();
        final Filter filter2 = FilterUtils.getFilter();
        final Page<Filter> page = new PageImpl<>(Lists.newArrayList(filter1, filter2));
        final PagedResources<Resource<Filter>> expected = PagedResourcesUtils.buildPagedResourcesFromPage(page);

        when(filterManager.getFiltersByUserId(userId, pageable)).thenReturn(page);
        when(filterPagedResourcesAssembler.toResource(page)).thenReturn(expected);

        // When
        final PagedResources<Resource<Filter>> result = filterResource.getFilters(userId, pageable);

        // Then
        verify(filterPagedResourcesAssembler).toResource(page);
        verify(filterManager).getFiltersByUserId(userId, pageable);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getFilters_whenAllIsFineAndNoFilter_shouldCallManagerAndReturnEmptyPage() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(1, 10);
        final Page<Filter> page = new PageImpl<>(Collections.emptyList());
        final PagedResources<Resource<Filter>> expected = PagedResourcesUtils.buildPagedResourcesFromPage(page);

        when(filterManager.getFiltersByUserId(userId, pageable)).thenReturn(page);
        when(filterPagedResourcesAssembler.toResource(page)).thenReturn(expected);

        // When
        final PagedResources<Resource<Filter>> result = filterResource.getFilters(userId, pageable);

        // Then
        verify(filterManager).getFiltersByUserId(userId, pageable);
        assertThat(result).isEqualTo(expected);
    }


    @Test
    public void getFilter_whenFilter_shouldCallManagerAndReturnFilter() throws Exception {
        // Given
        final Filter filter =  FilterUtils.getFilter();

        when(filterManager.getFilterByUserIdAndName(filter.getUserId(), filter.getName())).thenReturn(filter);

        // When
        final Filter result = filterResource.getFilter(filter.getUserId(), filter.getName());

        // Then
        verify(filterManager).getFilterByUserIdAndName(filter.getUserId(), filter.getName());
        assertThat(result).isEqualTo(filter);
    }

    @Test
    public void deleteFilter_whenFilter_shouldCallManagerAndReturnOk() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String filterName = UUID.randomUUID().toString();

        // When
        filterResource.deleteFilter(userId, filterName);

        // Then
        verify(filterManager).deleteFilterByUserIdAndName(userId, filterName);
    }
}
