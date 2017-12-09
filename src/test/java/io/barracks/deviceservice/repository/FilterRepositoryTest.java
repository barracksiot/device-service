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
import io.barracks.deviceservice.model.Filter;
import io.barracks.deviceservice.repository.exception.FilterCreationFailedException;
import io.barracks.deviceservice.utils.FilterUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class FilterRepositoryTest extends MongoRepositoryTest {
    private FilterRepositoryImpl filterRepository;

    public FilterRepositoryTest() {
        super(Filter.class.getAnnotation(Document.class).collection());
    }


    @Before
    public void setUp() throws Exception {
        super.setUp();
        MongoTemplate mongoTemplate = new MongoTemplate(getMongo(), getDatabaseName());
        filterRepository = spy(new FilterRepositoryImpl(mongoTemplate));
    }

    @Test
    public void saveFilter_whenNameAlreadyTaken_shouldThrowException() {
        // Given
        final Filter filter = getFilter();
        final Filter copy = filter.toBuilder().build();
        filterRepository.createFilter(filter);

        // Then When
        assertThatExceptionOfType(FilterCreationFailedException.class).isThrownBy(() ->
                filterRepository.createFilter(copy)
        );
    }

    @Test
    public void saveFilter_shouldReturnSameObjectWithDates() {
        // Given
        final Filter filter = getFilter();
        final Filter copy = filter.toBuilder().build();

        // Then
        final Filter result = filterRepository.createFilter(copy);

        // When
        assertThat(result).isEqualTo(copy.toBuilder().created(result.getCreated()).updated(copy.getUpdated()).build());
    }

    @Test
    public void getFilters_whenNoFilter_shouldReturnEmptyList() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);

        // When
        final Page<Filter> result = filterRepository.getFiltersByUserId(userId, pageable);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void getFilters_whenFilters_shouldReturnFiltersList() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final List<Filter> expected = getFilters(userId).stream().map(filter -> filterRepository.createFilter(filter)).collect(Collectors.toList());

        // When
        final Page<Filter> result = filterRepository.getFiltersByUserId(userId, pageable);

        // Then
        assertThat(result).containsAll(expected);
    }

    @Test
    public void getFilters_whenFiltersForMoreThanOneUser_shouldReturnFiltersOfCurrentUserOnly() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 20);
        final List<Filter> expected = getFilters(userId);
        final List<Filter> otherFilters = getFilters(UUID.randomUUID().toString());

        expected.forEach(filter -> filterRepository.createFilter(filter));
        otherFilters.forEach(filter -> filterRepository.createFilter(filter));

        // When
        final Page<Filter> result = filterRepository.getFiltersByUserId(userId, pageable);

        // Then
        assertThat(result).containsAll(expected);
    }

    @Test
    public void getFilters_whenManyFilters_shouldReturnPagedList() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(1, 5);
        final List<Filter> expected = getFilters(userId).stream().map(filter -> filterRepository.createFilter(filter)).collect(Collectors.toList());

        // When
        final Page<Filter> result = filterRepository.getFiltersByUserId(userId, pageable);

        // Then
        assertThat(result).hasSize(5).isSubsetOf(expected);
    }


    @Test
    public void getFilterByUserIdAndName_whenFilter_shouldReturnFilter(){
        // Given
        final Filter expected = getFilter();
        filterRepository.createFilter(expected);

        // When
        final Optional<Filter> result = filterRepository.getFilterByUserIdAndName(expected.getUserId(), expected.getName());

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(Optional.of(expected));
    }



    @Test
    public void getFilterByUserIdAndName_whenNoFilter_shouldReturnNull(){
        // Given
        final Filter expected = getFilter();

        // When
        final Optional<Filter> result = filterRepository.getFilterByUserIdAndName(expected.getUserId(), expected.getName());

        // Then
        assertThat(result).isEqualTo(Optional.empty());
    }


    @Test
    public void deleteFilterByUserIdAndName_whenFilter_shouldDeleteFilter(){
        // Given
        final Filter expected = getFilter();
        filterRepository.createFilter(expected);

        // When
        filterRepository.deleteFilterByUserIdAndName(expected.getUserId(), expected.getName());

        // Then
        final Optional<Filter> result = filterRepository.getFilterByUserIdAndName(expected.getUserId(), expected.getName());
        assertThat(result).isEqualTo(Optional.empty());
    }

    private List<Filter> getFilters(String userId) {
        return IntStream.range(0, 10)
                .mapToObj((index) -> getFilter(userId))
                .collect(Collectors.toList());
    }

    private Filter getFilter() {
        return FilterUtils.getFilter().toBuilder()
                .id(null)
                .name(UUID.randomUUID().toString())
                .userId(UUID.randomUUID().toString())
                .created(null)
                .updated(null)
                .build();
    }

    private Filter getFilter(String userId) {
        return getFilter().toBuilder()
                .userId(userId)
                .build();
    }

}
