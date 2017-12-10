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
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.barracks.deviceservice.manager.exception.FilterNotFoundException;
import io.barracks.deviceservice.manager.exception.MatchingFilterNotFoundException;
import io.barracks.deviceservice.manager.exception.NoMatchingFilterFoundException;
import io.barracks.deviceservice.model.DeviceRequest;
import io.barracks.deviceservice.model.Filter;
import io.barracks.deviceservice.model.operator.Operator;
import io.barracks.deviceservice.repository.FilterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FilterManager {

    private final FilterRepository filterRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public FilterManager(FilterRepository filterRepository, ObjectMapper objectMapper) {
        this.filterRepository = filterRepository;
        this.objectMapper = objectMapper;
    }

    public Filter createFilter(String userId, Filter filter) {
        final Filter toSave = filter.toBuilder().userId(userId).build();
        return filterRepository.createFilter(toSave);
    }

    public Page<Filter> getFiltersByUserId(String userId, Pageable pageable) {
        return filterRepository.getFiltersByUserId(userId, pageable);
    }

    public Filter getFilterByUserIdAndName(String userId, String name) {
        return filterRepository.getFilterByUserIdAndName(userId, name).orElseThrow(() -> new FilterNotFoundException(userId, name));
    }

    public void deleteFilterByUserIdAndName(String userId, String name) {
        filterRepository.deleteFilterByUserIdAndName(userId, name);
    }

    public Filter updateFilter(String userId, String name, Operator query) {
        final Filter filter = filterRepository.getFilterByUserIdAndName(userId, name).get();
        return filterRepository.updateFilter(filter, query);
    }

    public List<Filter> findMatchingFilters(String userId, String unitId, List<String> names, DeviceRequest event) {
        return getMatchingFilterStream(userId, unitId, names, event)
                .collect(Collectors.toList());
    }

    public Filter findFirstMatchingFilter(String userId, String unitId, List<String> names, DeviceRequest event) {
        return getMatchingFilterStream(userId, unitId, names, event)
                .findFirst().orElseThrow(() -> new NoMatchingFilterFoundException(userId, unitId, names, event));
    }

    private Stream<Filter> getMatchingFilterStream(String userId, String unitId, List<String> names, DeviceRequest event) {
        final JsonNode deviceJson = getDeviceJson(unitId, event);
        return names.stream()
                .map(filterName -> filterRepository.getFilterByUserIdAndName(userId, filterName).orElseThrow(() -> new MatchingFilterNotFoundException(userId, filterName)))
                .filter(filter -> filter.getQuery().matches(deviceJson));
    }

    JsonNode getDeviceJson(String unitId, DeviceRequest request) {
        final ObjectNode result = objectMapper.createObjectNode();
        result.put("unitId", unitId);
        result.set("request", objectMapper.valueToTree(request));
        // TODO Clean this up, and handle receptionDate
        return result;
    }
}
