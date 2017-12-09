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
import io.barracks.deviceservice.model.DeviceComponentRequest;
import io.barracks.deviceservice.model.Filter;
import io.barracks.deviceservice.repository.FilterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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

    public List<Filter> findMatchingFilters(List<String> names, DeviceComponentRequest event) {
        final JsonNode deviceJson = getDeviceJson(event);
        return names.stream()
                .map(filterName -> filterRepository.getFilterByUserIdAndName(event.getUserId(), filterName).orElseThrow(() -> new MatchingFilterNotFoundException(event.getUserId(), filterName)))
                .filter(filter -> filter.getQuery().matches(deviceJson))
                .collect(Collectors.toList());
    }

    public Filter findFirstMatchingFilter(List<String> names, DeviceComponentRequest event) {
        final JsonNode deviceJson = getDeviceJson(event);
        return names.stream()
                .map(filterName -> filterRepository.getFilterByUserIdAndName(event.getUserId(), filterName).orElseThrow(() -> new MatchingFilterNotFoundException(event.getUserId(), filterName)))
                .filter(filter -> filter.getQuery().matches(deviceJson))
                .findFirst().orElseThrow(() -> new NoMatchingFilterFoundException(event.getUserId(), names, deviceJson));
    }

    JsonNode getDeviceJson(DeviceComponentRequest event) {
        final ObjectNode result = objectMapper.createObjectNode();
        result.put("userId", event.getUserId());
        result.put("unitId", event.getUnitId());
        result.set("lastEvent", objectMapper.valueToTree(event));
        return result;
    }

    public void deleteFilterByUserIdAndName(String userId, String name) {
        filterRepository.deleteFilterByUserIdAndName(userId, name);
    }
}
