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

import io.barracks.deviceservice.model.Filter;
import io.barracks.deviceservice.model.operator.Operator;
import io.barracks.deviceservice.repository.exception.FilterCreationFailedException;
import io.barracks.deviceservice.utils.FilterUtils;
import io.barracks.deviceservice.utils.OperatorUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@RunWith(SpringRunner.class)
@BarracksRepositoryTest
public class FilterRepositoryTest {
    @Autowired
    private FilterRepository filterRepository;

    @Before
    public void setUp() throws Exception {
        filterRepository.deleteAll();
    }

    @Test
    public void createFilter_whenNameAlreadyTaken_shouldThrowException() {
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
    public void createFilter_shouldReturnSameObjectWithDates() {
        // Given
        final Filter filter = getFilter();
        final Filter copy = filter.toBuilder().build();

        // Then
        final Filter result = filterRepository.createFilter(copy);

        // When
        assertThat(result).isEqualTo(copy.toBuilder()
                .created(result.getCreated().withOffsetSameInstant(ZoneOffset.UTC))
                .updated(copy.getUpdated().withOffsetSameInstant(ZoneOffset.UTC))
                .build()
        );
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
        final List<Filter> expected = createFilters(userId);

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
        final List<Filter> expected = createFilters(userId);
        createFilters(UUID.randomUUID().toString());

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
        final List<Filter> expected = createFilters(userId);

        // When
        final Page<Filter> result = filterRepository.getFiltersByUserId(userId, pageable);

        // Then
        assertThat(result).hasSize(5).isSubsetOf(expected);
    }

    @Test
    public void getFilterByUserIdAndName_whenFilter_shouldReturnFilter() {
        // Given
        final Filter filter = getFilter();
        final Filter expected = filterRepository.createFilter(filter);

        // When
        final Optional<Filter> result = filterRepository.getFilterByUserIdAndName(filter.getUserId(), filter.getName());

        // Then
        assertThat(result).isPresent().contains(expected);
    }

    @Test
    public void getFilterByUserIdAndName_whenNoFilter_shouldReturnNull() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String name = UUID.randomUUID().toString();

        // When
        final Optional<Filter> result = filterRepository.getFilterByUserIdAndName(userId, name);

        // Then
        assertThat(result).isEqualTo(Optional.empty());
    }

    @Test
    public void deleteFilterByUserIdAndName_whenFilter_shouldDeleteFilter() {
        // Given
        final Filter expected = getFilter();
        filterRepository.createFilter(expected);

        // When
        filterRepository.deleteFilterByUserIdAndName(expected.getUserId(), expected.getName());

        // Then
        final Optional<Filter> result = filterRepository.getFilterByUserIdAndName(expected.getUserId(), expected.getName());
        assertThat(result).isEqualTo(Optional.empty());
    }

    @Test
    public void updateFilter_shouldReturnObjectWithUpdatedQuery() {
        // Given
        final Filter filter = getFilter();
        filterRepository.createFilter(filter);
        final Operator query = OperatorUtils.getOperator(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        final Filter updatedFilter = filter.toBuilder().query(query).build();

        // Then
        final Filter result = filterRepository.updateFilter(filter, query);

        // When
        assertThat(result).isEqualTo(updatedFilter.toBuilder()
                .created(result.getCreated().withOffsetSameInstant(ZoneOffset.UTC))
                .updated(updatedFilter.getUpdated().withOffsetSameInstant(ZoneOffset.UTC))
                .build()
        );
    }

    private List<Filter> createFilters(String userId) {
        return IntStream.range(0, 10)
                .mapToObj((index) -> getFilter(userId))
                .map(filter -> filterRepository.createFilter(filter))
                .collect(Collectors.toList());
    }

    private Filter getFilter() {
        return getFilter(UUID.randomUUID().toString());
    }

    private Filter getFilter(String userId) {
        return FilterUtils.getFilter().toBuilder()
                .id(null)
                .name(UUID.randomUUID().toString())
                .userId(userId)
                .created(null)
                .updated(null)
                .build();
    }

}
