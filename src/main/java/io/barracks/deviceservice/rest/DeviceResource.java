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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.deviceservice.manager.DeviceManager;
import io.barracks.deviceservice.model.Device;
import io.barracks.deviceservice.model.DeviceConfiguration;
import io.barracks.deviceservice.model.DeviceEvent;
import io.barracks.deviceservice.model.operator.Operator;
import io.barracks.deviceservice.rest.exception.BarracksQueryFormatException;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Optional;

@RestController
@RequestMapping("/devices")
public class DeviceResource {
    private final PagedResourcesAssembler<Device> deviceAssembler;
    private final PagedResourcesAssembler<DeviceEvent> deviceEventAssembler;
    private final DeviceManager deviceManager;
    private final ObjectMapper objectMapper;

    @Autowired
    public DeviceResource(ObjectMapper objectMapper, DeviceManager deviceManager, PagedResourcesAssembler<Device> deviceAssembler, PagedResourcesAssembler<DeviceEvent> deviceEventAssembler) {
        this.objectMapper = objectMapper;
        this.deviceManager = deviceManager;
        this.deviceAssembler = deviceAssembler;
        this.deviceEventAssembler = deviceEventAssembler;
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    public PagedResources<Resource<Device>> getDevices(
            @NotBlank @RequestParam(value = "userId") String userId,
            @RequestParam(value = "query", defaultValue = "") String query,
            Pageable pageable) {
        try {
            Optional<Operator> operator = Optional.empty();
            if (!StringUtils.isEmpty(query)) {
                operator = Optional.of(objectMapper.readValue(query, Operator.class));
            }
            return deviceAssembler.toResource(deviceManager.getDevicesByUserId(userId, operator, pageable));
        } catch (IOException e) {
            throw new BarracksQueryFormatException(query, e);
        }
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    public DeviceEvent addDeviceEvent(@RequestBody DeviceEvent source) {
        return deviceManager.saveDeviceEvent(source);
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, path = "/{unitId}")
    public Device getDevice(
            @PathVariable("unitId") String unitId,
            @NotBlank @RequestParam("userId") String userId) {
        return deviceManager.getDeviceByUserIdAndUnitId(userId, unitId);
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, path = "/{unitId}/events")
    public PagedResources<Resource<DeviceEvent>> getDeviceEventsByUnitId(
            @PathVariable("unitId") String unitId,
            @NotBlank @RequestParam(value = "userId") String userId,
            @RequestParam(value = "onlyChanged", required = false, defaultValue = "true") boolean onlyChanged,
            Pageable pageable) {
        return deviceEventAssembler.toResource(deviceManager.getDeviceEventsByUnitId(userId, unitId, onlyChanged, pageable));
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, path = "/{unitId}/configuration")
    public DeviceConfiguration getDeviceConfiguration(
            @PathVariable("unitId") String unitId,
            @NotBlank @RequestParam("userId") String userId) {
        return deviceManager.getDeviceConfiguration(userId, unitId);
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.POST, path = "/{unitId}/configuration")
    public DeviceConfiguration setDeviceConfiguration(
            @PathVariable("unitId") String unitId,
            @NotBlank @RequestParam("userId") String userId,
            @RequestBody DeviceConfiguration configuration) {
        return deviceManager.saveDeviceConfiguration(userId, unitId, configuration);
    }
}
