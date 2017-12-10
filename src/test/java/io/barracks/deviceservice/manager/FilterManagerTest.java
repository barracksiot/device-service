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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.barracks.deviceservice.manager.exception.FilterNotFoundException;
import io.barracks.deviceservice.manager.exception.MatchingFilterNotFoundException;
import io.barracks.deviceservice.manager.exception.NoMatchingFilterFoundException;
import io.barracks.deviceservice.model.DeviceRequest;
import io.barracks.deviceservice.model.Filter;
import io.barracks.deviceservice.model.operator.Operator;
import io.barracks.deviceservice.repository.FilterRepository;
import io.barracks.deviceservice.utils.DeviceRequestUtils;
import io.barracks.deviceservice.utils.FilterUtils;
import io.barracks.deviceservice.utils.OperatorUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FilterManagerTest {
    @Mock
    private FilterRepository filterRepository;
    private ObjectMapper objectMapper = new ObjectMapper();
    private FilterManager filterManager;

    @Before
    public void setup() {
        filterManager = spy(new FilterManager(filterRepository, objectMapper));
    }

    @Test
    public void createFilter_shouldCallRepository_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Filter filter = FilterUtils.getFilter();
        final Filter toCreate = filter.toBuilder().userId(userId).build();
        final Filter expected = FilterUtils.getFilter();
        when(filterRepository.createFilter(toCreate)).thenReturn(expected);

        // When
        final Filter result = filterManager.createFilter(userId, filter);

        // Then
        verify(filterRepository).createFilter(toCreate);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getFilters_shouldCallRepository_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Filter filter1 = FilterUtils.getFilter();
        final Filter filter2 = FilterUtils.getFilter();
        final List<Filter> response = new ArrayList<Filter>() {{
            add(filter1);
            add(filter2);
        }};
        final Page<Filter> expected = new PageImpl<>(response, pageable, 2);

        when(filterRepository.getFiltersByUserId(userId, pageable)).thenReturn(expected);

        // When
        final Page<Filter> result = filterManager.getFiltersByUserId(userId, pageable);

        // Then
        verify(filterRepository).getFiltersByUserId(userId, pageable);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getDeviceJson_shouldCreateDeviceWithUnitIdUserIdAndLatestEvent() throws Exception {
        // Given
        final String unitId = UUID.randomUUID().toString();
        final DeviceRequest event = DeviceRequestUtils.getDeviceRequest();

        // When
        final ObjectNode result = (ObjectNode) filterManager.getDeviceJson(unitId, event);

        // Then
        assertThat(result.get("unitId").asText()).isEqualTo(unitId);
        assertThat(result.get("request")).isEqualTo(objectMapper.valueToTree(event));
    }

    @Test
    public void findMatchingFilters_shouldCheckDeviceAgainstAllFilters_andReturnMatchingList() {
        // Given
        final DeviceRequest deviceEvent = DeviceRequestUtils.getDeviceRequest();
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final JsonNode deviceJson = new ObjectNode(JsonNodeFactory.instance);
        final List<String> name = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        final Operator query1 = mock(Operator.class);
        final Operator query2 = mock(Operator.class);
        final Filter filter1 = FilterUtils.getFilter().toBuilder().query(query1).build();
        final Filter filter2 = FilterUtils.getFilter().toBuilder().query(query2).build();
        doReturn(deviceJson).when(filterManager).getDeviceJson(unitId, deviceEvent);
        doReturn(Optional.of(filter1)).when(filterRepository).getFilterByUserIdAndName(userId, name.get(0));
        doReturn(Optional.of(filter2)).when(filterRepository).getFilterByUserIdAndName(userId, name.get(1));
        doReturn(false).when(query1).matches(deviceJson);
        doReturn(true).when(query2).matches(deviceJson);

        // When
        final List<Filter> results = filterManager.findMatchingFilters(userId, unitId, name, deviceEvent);

        // Then
        verify(filterManager).getDeviceJson(unitId, deviceEvent);
        verify(filterRepository).getFilterByUserIdAndName(userId, name.get(0));
        verify(filterRepository).getFilterByUserIdAndName(userId, name.get(1));
        verify(query1).matches(deviceJson);
        verify(query2).matches(deviceJson);
        assertThat(results).containsOnly(filter2);
    }

    @Test
    public void findMatchingFilters_whenFilterNotFound_shouldThrowException() {
        // Given
        final DeviceRequest deviceEvent = DeviceRequestUtils.getDeviceRequest();
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final JsonNode deviceJson = new ObjectNode(JsonNodeFactory.instance);
        final List<String> names = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        doReturn(deviceJson).when(filterManager).getDeviceJson(unitId, deviceEvent);
        doReturn(Optional.empty()).when(filterRepository).getFilterByUserIdAndName(userId, names.get(0));

        // Then When
        assertThatExceptionOfType(MatchingFilterNotFoundException.class)
                .isThrownBy(() -> filterManager.findMatchingFilters(userId, unitId, names, deviceEvent));
        verify(filterManager).getDeviceJson(unitId, deviceEvent);
        verify(filterRepository).getFilterByUserIdAndName(userId, names.get(0));
        verify(filterRepository, times(0)).getFilterByUserIdAndName(userId, names.get(1));
    }

    @Test
    public void findFirstMatchingFilter_shouldCheckDeviceAgainstFilters_andReturnFirstMatching() {
        // Given
        final DeviceRequest deviceEvent = DeviceRequestUtils.getDeviceRequest();
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final JsonNode deviceJson = new ObjectNode(JsonNodeFactory.instance);
        final List<String> names = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        final Operator query1 = mock(Operator.class);
        final Operator query2 = mock(Operator.class);
        final Filter filter1 = FilterUtils.getFilter().toBuilder().query(query1).build();
        final Filter filter2 = FilterUtils.getFilter().toBuilder().query(query2).build();
        doReturn(deviceJson).when(filterManager).getDeviceJson(unitId, deviceEvent);
        doReturn(Optional.of(filter1)).when(filterRepository).getFilterByUserIdAndName(userId, names.get(0));
        doReturn(Optional.of(filter2)).when(filterRepository).getFilterByUserIdAndName(userId, names.get(1));
        doReturn(false).when(query1).matches(deviceJson);
        doReturn(true).when(query2).matches(deviceJson);

        // When
        final Filter results = filterManager.findFirstMatchingFilter(userId, unitId, names, deviceEvent);

        // Then
        verify(filterManager).getDeviceJson(unitId, deviceEvent);
        verify(filterRepository).getFilterByUserIdAndName(userId, names.get(0));
        verify(filterRepository).getFilterByUserIdAndName(userId, names.get(1));
        verify(query1).matches(deviceJson);
        verify(query2).matches(deviceJson);
        assertThat(results).isEqualTo(filter2);
    }

    @Test
    public void findFirstMatchingFilter_whenNoMatchingFilter_shouldThrowException() {
        // Given
        final DeviceRequest deviceEvent = DeviceRequestUtils.getDeviceRequest();
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final JsonNode deviceJson = new ObjectNode(JsonNodeFactory.instance);
        final List<String> names = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        final Operator query = mock(Operator.class);
        final Filter filter1 = FilterUtils.getFilter().toBuilder().query(query).build();
        final Filter filter2 = FilterUtils.getFilter().toBuilder().query(query).build();
        doReturn(deviceJson).when(filterManager).getDeviceJson(unitId, deviceEvent);
        doReturn(Optional.of(filter1)).when(filterRepository).getFilterByUserIdAndName(userId, names.get(0));
        doReturn(Optional.of(filter2)).when(filterRepository).getFilterByUserIdAndName(userId, names.get(1));
        doReturn(false).when(query).matches(deviceJson);

        // Then When
        assertThatExceptionOfType(NoMatchingFilterFoundException.class)
                .isThrownBy(() -> filterManager.findFirstMatchingFilter(userId, unitId, names, deviceEvent));
        verify(filterManager).getDeviceJson(unitId, deviceEvent);
        verify(filterRepository).getFilterByUserIdAndName(userId, names.get(0));
        verify(filterRepository).getFilterByUserIdAndName(userId, names.get(1));
        verify(query, times(2)).matches(deviceJson);
    }

    @Test
    public void findFirstMatchingFilter_whenFilterNotFound_shouldThrowException() {
        // Given
        final DeviceRequest deviceEvent = DeviceRequestUtils.getDeviceRequest();
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final JsonNode deviceJson = new ObjectNode(JsonNodeFactory.instance);
        final List<String> names = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        doReturn(deviceJson).when(filterManager).getDeviceJson(unitId, deviceEvent);
        doReturn(Optional.empty()).when(filterRepository).getFilterByUserIdAndName(userId, names.get(0));
        doReturn(Optional.empty()).when(filterRepository).getFilterByUserIdAndName(userId, names.get(1));

        // Then When
        assertThatExceptionOfType(FilterNotFoundException.class)
                .isThrownBy(() -> filterManager.findFirstMatchingFilter(userId, unitId, names, deviceEvent));
        verify(filterManager).getDeviceJson(unitId, deviceEvent);
        verify(filterRepository).getFilterByUserIdAndName(userId, names.get(0));
        verify(filterRepository, times(0)).getFilterByUserIdAndName(userId, names.get(1));
    }

    @Test
    public void getFilterByUserIdAndName_whenFilter_shouldCallRepositoryAndReturnResult() {
        // Given
        final Filter filter = FilterUtils.getFilter();
        doReturn(Optional.of(filter)).when(filterRepository).getFilterByUserIdAndName(filter.getUserId(), filter.getName());

        // When
        final Filter result = filterManager.getFilterByUserIdAndName(filter.getUserId(), filter.getName());

        // Then
        verify(filterRepository).getFilterByUserIdAndName(filter.getUserId(), filter.getName());
        assertThat(result).isEqualTo(filter);
    }

    @Test
    public void getFilterByUserIdAndName_whenNoFilter_shouldCallRepositoryAndReturnResult() {
        // Given
        final Filter filter = FilterUtils.getFilter();
        doReturn(Optional.empty()).when(filterRepository).getFilterByUserIdAndName(filter.getUserId(), filter.getName());

        // Then / When
        assertThatExceptionOfType(FilterNotFoundException.class).isThrownBy(() -> filterManager.getFilterByUserIdAndName(filter.getUserId(), filter.getName()));
        verify(filterRepository).getFilterByUserIdAndName(filter.getUserId(), filter.getName());
    }

    @Test
    public void deleteFilter_whenFilter_shouldCallRepository() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String filterName = UUID.randomUUID().toString();

        // When
        filterManager.deleteFilterByUserIdAndName(userId, filterName);

        // Then
        verify(filterRepository).deleteFilterByUserIdAndName(userId, filterName);
    }

    @Test
    public void updateFilter_shouldCallRepository_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String name = UUID.randomUUID().toString();
        final Filter toUpdate = FilterUtils.getFilter().toBuilder().userId(userId).name(name).build();
        final Operator query = OperatorUtils.getOperator(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        final Filter expected = toUpdate.toBuilder().query(query).build();
        doReturn(Optional.of(toUpdate)).when(filterRepository).getFilterByUserIdAndName(userId, name);
        doReturn(expected).when(filterRepository).updateFilter(toUpdate, query);

        // When
        final Filter result = filterManager.updateFilter(userId, name, query);

        // Then
        verify(filterRepository).getFilterByUserIdAndName(userId, name);
        verify(filterRepository).updateFilter(toUpdate, query);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void updateFilter_shouldThrowException_whenFilterDoesNotExist() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String name = UUID.randomUUID().toString();
        final Operator query = OperatorUtils.getOperator(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        doReturn(Optional.empty()).when(filterRepository).getFilterByUserIdAndName(userId, name);

        // When
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(
                () -> filterManager.updateFilter(userId, name, query)
        );

        // Then
        verify(filterRepository).getFilterByUserIdAndName(userId, name);
    }

}
