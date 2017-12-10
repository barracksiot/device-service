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
import io.barracks.deviceservice.model.DeviceEvent;
import io.barracks.deviceservice.model.operator.Operator;
import io.barracks.deviceservice.rest.exception.BarracksQueryFormatException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;

import java.util.Arrays;
import java.util.UUID;

import static io.barracks.commons.test.PagedResourcesUtils.getDefaultPageable;
import static io.barracks.commons.test.PagedResourcesUtils.getPagedResourcesAssembler;
import static io.barracks.deviceservice.utils.DeviceEventUtils.getDeviceEvent;
import static io.barracks.deviceservice.utils.DeviceUtils.getDevice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeviceResourceTest {

    private DeviceResource resource;

    private ObjectMapper objectMapper;

    @Mock
    private DeviceManager manager;

    private PagedResourcesAssembler<Device> deviceAssembler = getPagedResourcesAssembler();

    private PagedResourcesAssembler<DeviceEvent> deviceEventAssembler = getPagedResourcesAssembler();

    @Before
    public void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        resource = new DeviceResource(objectMapper, manager, deviceAssembler, deviceEventAssembler);
        reset(manager);
    }

    @Test
    public void getDevices_whenOperatorIsNull_shouldCallManager_andReturnValue() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String query = null;
        final Pageable pageable = getDefaultPageable();
        final Page<Device> expected = new PageImpl<>(Arrays.asList(getDevice(), getDevice()));
        doReturn(expected).when(manager).getDevices(userId, null, pageable);

        // When
        final PagedResources<Resource<Device>> result = resource.getDevices(userId, query, pageable);

        // Then
        verify(manager).getDevices(userId, null, pageable);
        assertThat(result).isEqualTo(deviceAssembler.toResource(expected));
    }

    @Test
    public void getDevices_whenOperatorIsValid_shouldCallManager_AndReturnValue() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String query = "{ \"eq\" : { \"unitId\": \"abc123\" }}";
        final Operator operator = objectMapper.readValue(query, Operator.class);
        final Pageable pageable = getDefaultPageable();
        final Page<Device> expected = new PageImpl<>(Arrays.asList(getDevice(), getDevice()));
        doReturn(expected).when(manager).getDevices(userId, operator, pageable);

        // When
        final PagedResources<Resource<Device>> result = resource.getDevices(userId, query, pageable);

        // Then
        verify(manager).getDevices(userId, operator, pageable);
        assertThat(result).isEqualTo(deviceAssembler.toResource(expected));
    }

    @Test
    public void getDevices_whenOperatorIsInvalid_shouldThrowException() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String query = "I'm not valid";
        final Pageable pageable = getDefaultPageable();

        // Then / When
        assertThatExceptionOfType(BarracksQueryFormatException.class)
                .isThrownBy(() -> resource.getDevices(userId, query, pageable));
        verifyZeroInteractions(manager);
    }

    @Test
    public void addDeviceEvent_shouldForwardCallToTheManager_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceEvent event = getDeviceEvent();
        final DeviceEvent called = event.toBuilder().unitId(unitId).userId(userId).build();
        final DeviceEvent expected = getDeviceEvent();
        doReturn(expected).when(manager).saveDeviceEvent(called);

        // When
        final DeviceEvent result = resource.addDeviceEvent(userId, unitId, event);

        // Then
        verify(manager).saveDeviceEvent(called);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getDevice_shouldForwardCallToTheManager_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Device expected = getDevice();
        doReturn(expected).when(manager).getDevice(userId, unitId);

        // When
        final Device result = resource.getDevice(userId, unitId);

        // Then
        verify(manager).getDevice(userId, unitId);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getDeviceEvents_shouldForwardCallToTheManager_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Page<DeviceEvent> expected = new PageImpl<>(Arrays.asList(getDeviceEvent(), getDeviceEvent()));
        final Pageable pageable = getDefaultPageable();
        final boolean onlyChanged = true;
        doReturn(expected).when(manager).getDeviceEvents(userId, unitId, onlyChanged, pageable);

        // When
        final PagedResources<Resource<DeviceEvent>> result = resource.getDeviceEvents(userId, unitId, onlyChanged, pageable);

        // Then
        verify(manager).getDeviceEvents(userId, unitId, onlyChanged, pageable);
        assertThat(result).isEqualTo(deviceEventAssembler.toResource(expected));
    }
}
