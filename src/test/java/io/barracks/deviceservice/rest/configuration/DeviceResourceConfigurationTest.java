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

package io.barracks.deviceservice.rest.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.barracks.commons.test.PagedResourcesUtils;
import io.barracks.commons.util.Endpoint;
import io.barracks.deviceservice.model.Device;
import io.barracks.deviceservice.model.DeviceEvent;
import io.barracks.deviceservice.model.operator.comparison.EqualOperator;
import io.barracks.deviceservice.rest.DeviceResource;
import io.barracks.deviceservice.utils.DeviceUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.FileCopyUtils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.barracks.commons.test.PagedResourcesUtils.getDefaultPageable;
import static io.barracks.deviceservice.utils.DeviceEventUtils.getDeviceEvent;
import static io.barracks.deviceservice.utils.DeviceUtils.getDevice;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@BarracksResourceTest(controllers = DeviceResource.class, outputDir = "build/generated-snippets/devices")
public class DeviceResourceConfigurationTest {

    private static final String baseUrl = "https://not.barracks.io";

    private static final Endpoint GET_DEVICES_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/devices");
    private static final Endpoint GET_DEVICES_WITH_QUERY_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/devices", "query={query}");
    private static final Endpoint CREATE_DEVICE_EVENT_ENDPOINT = Endpoint.from(HttpMethod.POST, "/owners/{userId}/devices/{unitId}/events");
    private static final Endpoint GET_DEVICE_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/devices/{unitId}");
    private static final Endpoint GET_DEVICE_EVENTS_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/devices/{unitId}/events");
    private static final Endpoint GET_DEVICE_EVENTS_WITH_ONLY_CHANGED_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/devices/{unitId}/events", "onlyChanged={onlyChanged}");

    @MockBean
    private DeviceResource deviceResource;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Value("classpath:io/barracks/deviceservice/event.json")
    private org.springframework.core.io.Resource deviceEvent;

    @Value("classpath:io/barracks/deviceservice/event-documentation.json")
    private org.springframework.core.io.Resource deviceEventDoc;

    private PagedResourcesAssembler<Device> deviceAssembler = PagedResourcesUtils.getPagedResourcesAssembler();

    private PagedResourcesAssembler<DeviceEvent> deviceEventAssembler = PagedResourcesUtils.getPagedResourcesAssembler();

    @Test
    public void documentGetDevices() throws Exception {
        // Given
        final Endpoint endpoint = GET_DEVICES_WITH_QUERY_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final List<Device> devices = Arrays.asList(getDevice(userId), getDevice(userId));
        final long total = 42L;
        final String query = mapper.writeValueAsString(new EqualOperator("unitId", "abc123"));
        final PagedResources expected = deviceAssembler.toResource(new PageImpl<>(devices, getDefaultPageable(), total));
        doReturn(expected).when(deviceResource).getDevices(eq(userId), eq(query), any());

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(
                        endpoint.getMethod(),
                        baseUrl + endpoint.getPath() + "?" + endpoint.getQuery(),
                        userId,
                        query
                )
        );

        // Then
        verify(deviceResource).getDevices(eq(userId), eq(query), any());
        result.andExpect(status().isOk())
                .andDo(document(
                        "list",
                        pathParameters(
                                parameterWithName("userId").description("ID of the user")
                        ),
                        requestParameters(
                                parameterWithName("query").optional().description("Optional query for filtering specific devices")
                        ),
                        responseFields(
                                fieldWithPath("_embedded.devices").description("The list of devices found"),
                                fieldWithPath("_links").ignored(),
                                fieldWithPath("page").ignored()
                        )
                ));
    }

    @Test
    public void getDevices_withValidParameters_shouldCallGetDevices_andReturnResult() throws Exception {
        // Given
        final Endpoint endpoint = GET_DEVICES_WITH_QUERY_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final List<Device> devices = Arrays.asList(getDevice(userId), getDevice(userId));
        final Pageable pageable = getDefaultPageable();
        final long total = 42L;
        final String query = mapper.writeValueAsString(new EqualOperator("unitId", "abc123"));
        final PagedResources expected = deviceAssembler.toResource(new PageImpl<>(devices, pageable, total));
        doReturn(expected).when(deviceResource).getDevices(userId, query, pageable);

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).pageable(pageable).getURI(userId, query))
        );

        // Then
        verify(deviceResource).getDevices(userId, query, pageable);
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.devices", hasSize(devices.size())));
    }

    @Test
    public void getDevices_withoutQuery_shouldCallGetDevices_andReturnResult() throws Exception {
        // Given
        final Endpoint endpoint = GET_DEVICES_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final List<Device> devices = Arrays.asList(getDevice(userId), getDevice(userId));
        final Pageable pageable = getDefaultPageable();
        final long total = 42L;
        final PagedResources<Resource<Device>> expected = deviceAssembler.toResource(new PageImpl<>(devices, pageable, total));
        doReturn(expected).when(deviceResource).getDevices(userId, "", pageable);

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).pageable(pageable).getURI(userId))
        );

        // Then
        verify(deviceResource).getDevices(userId, "", pageable);
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.devices", hasSize(devices.size())));
    }

    @Test
    public void getDevices_withEmptyQuery_shouldCallGetDevices_andReturnResult() throws Exception {
        // Given
        final Endpoint endpoint = GET_DEVICES_WITH_QUERY_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final List<Device> devices = Arrays.asList(getDevice(userId), getDevice(userId));
        final Pageable pageable = getDefaultPageable();
        final long total = 42L;
        final String query = "";
        final PagedResources<Resource<Device>> expected = deviceAssembler.toResource(new PageImpl<>(devices, pageable, total));
        doReturn(expected).when(deviceResource).getDevices(userId, query, pageable);

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).pageable(pageable).getURI(userId, query))
        );

        // Then
        verify(deviceResource).getDevices(userId, query, pageable);
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.devices", hasSize(devices.size())));
    }

    @Test
    public void documentAddDeviceEvent() throws Exception {
        // Given
        final DeviceEvent source = mapper.readValue(deviceEventDoc.getInputStream(), DeviceEvent.class);
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Endpoint endpoint = CREATE_DEVICE_EVENT_ENDPOINT;
        doReturn(source).when(deviceResource).addDeviceEvent(userId, unitId, source);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders
                        .request(
                                endpoint.getMethod(),
                                baseUrl + endpoint.getPath(),
                                userId,
                                unitId
                        )
                        .content(FileCopyUtils.copyToByteArray(deviceEventDoc.getInputStream()))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(deviceResource).addDeviceEvent(userId, unitId, source);
        result.andExpect(status().isCreated())
                .andDo(
                        document(
                                "add-event",
                                pathParameters(
                                        parameterWithName("userId").description("ID of the user"),
                                        parameterWithName("unitId").description("Unique ID of the device")
                                ),
                                requestFields(
                                        fieldWithPath("request").description("The request sent by the device"),
                                        fieldWithPath("response").description("The response given by Barracks")
                                )
                        )
                );
    }

    @Test
    public void postDeviceEvent_withValidParameters_shouldCallAddDeviceEvent_andReturnResult() throws Exception {
        // Given
        final DeviceEvent source = mapper.readValue(deviceEvent.getInputStream(), DeviceEvent.class);
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Endpoint endpoint = CREATE_DEVICE_EVENT_ENDPOINT;
        final DeviceEvent expected = getDeviceEvent();
        doReturn(expected).when(deviceResource).addDeviceEvent(userId, unitId, source);

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI(userId, unitId))
                        .content(FileCopyUtils.copyToByteArray(deviceEvent.getInputStream()))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(deviceResource).addDeviceEvent(userId, unitId, source);
        result.andExpect(status().isCreated())
                .andExpect(content().json(mapper.writeValueAsString(expected)));
    }

    @Test
    public void postDeviceEvent_withInvalidEvent_shouldReturnBadRequest() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Endpoint endpoint = CREATE_DEVICE_EVENT_ENDPOINT;

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI(userId, unitId))
                        .content("{}")
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // Then
        verifyZeroInteractions(deviceResource);
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void documentGetDevice() throws Exception {
        // Given
        final Endpoint endpoint = GET_DEVICE_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Device expected = DeviceUtils.getDevice();
        doReturn(expected).when(deviceResource).getDevice(userId, unitId);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(
                        endpoint.getMethod(),
                        baseUrl + endpoint.getPath(),
                        userId,
                        unitId
                )
        );

        // Then
        verify(deviceResource).getDevice(userId, unitId);
        result.andExpect(status().isOk())
                .andDo(
                        document(
                                "get",
                                pathParameters(
                                        parameterWithName("userId").description("ID of the user"),
                                        parameterWithName("unitId").description("Unique ID of the device")
                                ),
                                responseFields(
                                        fieldWithPath("userId").description("ID of the user"),
                                        fieldWithPath("unitId").description("Unique ID of the device"),
                                        fieldWithPath("firstSeen").description("First time the device was seen on Barracks"),
                                        fieldWithPath("lastEvent").description("Last event added for the device")
                                )
                        )
                );
    }

    @Test
    public void getDevice_withValidParameters_shouldCallGetDevice_andReturnResult() throws Exception {
        // Given
        final Endpoint endpoint = GET_DEVICE_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Device expected = DeviceUtils.getDevice();
        doReturn(expected).when(deviceResource).getDevice(userId, unitId);

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI(userId, unitId))
        );

        // Then
        verify(deviceResource).getDevice(userId, unitId);
        result.andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(expected)));
    }

    @Test
    public void documentGetDeviceEvents() throws Exception {
        // Given
        final Endpoint endpoint = GET_DEVICE_EVENTS_WITH_ONLY_CHANGED_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 20);
        final List<DeviceEvent> events = Arrays.asList(getDeviceEvent(), getDeviceEvent());
        final long total = 42L;
        final PagedResources<Resource<DeviceEvent>> page = deviceEventAssembler.toResource(new PageImpl<>(events, pageable, total));

        doReturn(page).when(deviceResource).getDeviceEvents(userId, unitId, true, pageable);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(
                        endpoint.getMethod(),
                        baseUrl + endpoint.getPath() + "?" + endpoint.getQuery(),
                        userId, unitId, true
                )
        );

        // Then
        verify(deviceResource).getDeviceEvents(userId, unitId, true, pageable);
        result.andExpect(status().isOk())
                .andDo(
                        document(
                                "list-events",
                                pathParameters(
                                        parameterWithName("userId").description("ID of the user"),
                                        parameterWithName("unitId").description("Unique ID of the device")
                                ),
                                requestParameters(
                                        parameterWithName("onlyChanged").optional().description("True for only events with a change, false for all events. (default : true)")
                                ),
                                responseFields(
                                        fieldWithPath("_embedded.events").description("The list of events for the device"),
                                        fieldWithPath("_links").ignored(),
                                        fieldWithPath("page").ignored()
                                )
                        )
                );
    }

    @Test
    public void getDeviceEvents_withValidParameters_shouldCallGetDeviceEventsWithOnlyChanged_andReturnResult() throws Exception {
        // Given
        final Endpoint endpoint = GET_DEVICE_EVENTS_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Pageable pageable = PagedResourcesUtils.getDefaultPageable();
        final List<DeviceEvent> events = Arrays.asList(getDeviceEvent(), getDeviceEvent());
        final long total = 42L;
        final PagedResources<Resource<DeviceEvent>> page = deviceEventAssembler.toResource(new PageImpl<>(events, pageable, total));

        doReturn(page).when(deviceResource).getDeviceEvents(userId, unitId, true, pageable);

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).pageable(pageable).getURI(userId, unitId))
        );

        // Then
        verify(deviceResource).getDeviceEvents(userId, unitId, true, pageable);
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.events", hasSize(events.size())));
    }

    @Test
    public void getDeviceEvents_withChanged_shouldCallGetDeviceEvents_andReturnResult() throws Exception {
        // Given
        final Endpoint endpoint = GET_DEVICE_EVENTS_WITH_ONLY_CHANGED_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final Pageable pageable = PagedResourcesUtils.getDefaultPageable();
        final List<DeviceEvent> events = Arrays.asList(getDeviceEvent(), getDeviceEvent());
        final long total = 42L;
        final PagedResources<Resource<DeviceEvent>> page = deviceEventAssembler.toResource(new PageImpl<>(events, pageable, total));
        doReturn(page).when(deviceResource).getDeviceEvents(userId, unitId, false, pageable);

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).pageable(pageable).getURI(userId, unitId, false))
        );

        // Then
        verify(deviceResource).getDeviceEvents(userId, unitId, false, pageable);
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.events", hasSize(events.size())));
    }

}
