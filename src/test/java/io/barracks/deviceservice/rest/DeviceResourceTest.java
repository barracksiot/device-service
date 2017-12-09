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
import io.barracks.commons.test.AnyBarracksClientException;
import io.barracks.commons.test.JsonResourceLoader;
import io.barracks.commons.test.ServiceClientTest;
import io.barracks.deviceservice.config.ExceptionConfig;
import io.barracks.deviceservice.manager.DeviceManager;
import io.barracks.deviceservice.model.Device;
import io.barracks.deviceservice.model.DeviceConfiguration;
import io.barracks.deviceservice.model.DeviceEvent;
import io.barracks.deviceservice.model.operator.Operator;
import net.minidev.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class DeviceResourceTest extends ServiceClientTest {

    private static final String GET_DEVICES_BY_USER_ID_ENDPOINT = "/devices?userId={userId}";
    private static final String GET_DEVICES_BY_USER_ID_AND_QUERY_ENDPOINT = "/devices?userId={userId}&query={query}";

    @Rule
    public final RestDocumentation restDocumentation = new RestDocumentation("build/generated-snippets");
    private DeviceResource deviceResource;
    private MockMvc mvc;
    private ObjectMapper objectMapper;

    @Mock
    private DeviceManager deviceManager;

    private HateoasPageableHandlerMethodArgumentResolver argumentResolver = new HateoasPageableHandlerMethodArgumentResolver();

    @Before
    public void setUp() throws Exception {
        final PagedResourcesAssembler<DeviceEvent> deviceEventAssembler = new PagedResourcesAssembler<>(argumentResolver, null);
        final PagedResourcesAssembler<Device> deviceAssembler = new PagedResourcesAssembler<>(argumentResolver, null);
        objectMapper = new ObjectMapper();
        deviceResource = new DeviceResource(objectMapper, deviceManager, deviceAssembler, deviceEventAssembler);
        final RestDocumentationResultHandler document = document("{method-name}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()));
        this.mvc = MockMvcBuilders
                .standaloneSetup(deviceResource)
                .setCustomArgumentResolvers(argumentResolver)
                .setHandlerExceptionResolvers(new ExceptionConfig().restExceptionResolver().build())
                .apply(documentationConfiguration(restDocumentation))
                .alwaysDo(document)
                .build();
        reset(deviceManager);
    }

    @Test
    public void createDevice_whenAllIsFine_shouldReturnCreatedDevice() throws Exception {
        // Given
        final JSONObject json = getJsonFromResource("request");
        final String userId = json.getAsString("userId");
        final String unitId = json.getAsString("unitId");
        final String versionId = json.getAsString("versionId");
        final String segmentId = UUID.randomUUID().toString();
        final String deviceIP = "123.456.78.9";
        final DeviceEvent toSave = DeviceEvent.builder()
                .userId(userId)
                .unitId(unitId)
                .versionId(versionId)
                .build();
        final DeviceEvent created = DeviceEvent.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .unitId(unitId)
                .versionId(versionId)
                .segmentId(segmentId)
                .receptionDate(new Date(123456789000L))
                .deviceIP(deviceIP)
                .build();
        final SimpleDateFormat sdf = new SimpleDateFormat(DeviceEvent.DATE_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("Z"));
        doReturn(created).when(deviceManager).saveDeviceEvent(any());

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.post("/devices/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.toJSONString())
        );

        // Then
        verify(deviceManager).saveDeviceEvent(toSave);
        result.andExpect(jsonPath("id").value(created.getId()))
                .andExpect(jsonPath("userId").value(created.getUserId()))
                .andExpect(jsonPath("unitId").value(created.getUnitId()))
                .andExpect(jsonPath("versionId").value(created.getVersionId()))
                .andExpect(jsonPath("receptionDate").value(sdf.format(created.getReceptionDate())))
                .andExpect(jsonPath("additionalProperties").value(new LinkedHashMap<>(created.getAdditionalProperties())))
                .andExpect(jsonPath("segmentId").value(created.getSegmentId()))
                .andExpect(jsonPath("deviceIP").value(deviceIP));
    }

    @Test
    public void createDevice_whenClientFails_shouldReturnErrorCode() throws Exception {
        // Given
        final JSONObject json = getJsonFromResource("request");
        final DeviceEvent source = DeviceEvent.builder()
                .userId(json.getAsString("userId"))
                .unitId(json.getAsString("unitId"))
                .versionId(json.getAsString("versionId"))
                .build();
        final SimpleDateFormat sdf = new SimpleDateFormat(DeviceEvent.DATE_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("Z"));
        doThrow(AnyBarracksClientException.from(HttpStatus.INTERNAL_SERVER_ERROR)).when(deviceManager).saveDeviceEvent(any());

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.post("/devices/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.toJSONString())
        );

        result.andExpect(status().isInternalServerError());
        verify(deviceManager).saveDeviceEvent(source);
    }

    @Test
    public void getDevicesByUserId_whenAllIsFineAndNoDevice_shouldCallManagerAndReturnEmptyPage() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        doReturn(new PageImpl<Device>(Collections.emptyList())).when(deviceManager).getDevicesByUserId(eq(userId), eq(Optional.empty()), any());

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get(GET_DEVICES_BY_USER_ID_ENDPOINT, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(deviceManager).getDevicesByUserId(eq(userId), eq(Optional.empty()), any());
        result.andExpect(status().isOk());
    }

    @Test
    public void getDevicesByUserIdAndQuery_whenQueryInParams_shouldCallManagerAndForwardTheQuery() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final JSONObject json = JsonResourceLoader.getJsonFromResource(getClass(), "query");
        final Operator operator = objectMapper.readValue(json.toJSONString(), Operator.class);
        doReturn(new PageImpl<Device>(Collections.emptyList())).when(deviceManager).getDevicesByUserId(eq(userId), eq(Optional.of(operator)), any());

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get(GET_DEVICES_BY_USER_ID_AND_QUERY_ENDPOINT, userId, json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(deviceManager).getDevicesByUserId(eq(userId), eq(Optional.of(operator)), any());
        result.andExpect(status().isOk());
    }

    @Test
    public void getDevicesByUserIdAndQuery_whenOperatorConversionFails_shouldReturn400BadRequest() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String invalidQuery = "invalidQuery";

        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get(GET_DEVICES_BY_USER_ID_AND_QUERY_ENDPOINT, userId, invalidQuery)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verifyZeroInteractions(deviceManager);
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void getDevices_whenAllIsFine_shouldCallManagerAndReturnDeviceList() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final JSONObject json = JsonResourceLoader.getJsonFromResource(getClass(), "query");
        final Operator operator = objectMapper.readValue(json.toJSONString(), Operator.class);
        final String unitId = UUID.randomUUID().toString();
        final Device device = Device.builder()
                .unitId(unitId)
                .userId(userId)
                .configuration(DeviceConfiguration.builder()
                        .unitId(unitId)
                        .userId(userId)
                        .build()
                )
                .build();
        doReturn(new PageImpl<>(Collections.singletonList(device))).when(deviceManager).getDevicesByUserId(eq(userId), eq(Optional.of(operator)), any());

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get(GET_DEVICES_BY_USER_ID_AND_QUERY_ENDPOINT, userId, json.toJSONString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(deviceManager).getDevicesByUserId(eq(userId), eq(Optional.of(operator)), any());
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.content[0].unitId").value(unitId))
                .andExpect(jsonPath("$.content[0].userId").value(userId));
    }

    @Test
    public void getDeviceConfiguration_whenFound_shouldReturnConfiguration() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceConfiguration deviceConfiguration = DeviceConfiguration.builder()
                .id(UUID.randomUUID().toString())
                .unitId(unitId)
                .userId(userId)
                .creationDate(new Date(123456789000L))
                .build();
        final SimpleDateFormat sdf = new SimpleDateFormat(DeviceConfiguration.DATE_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("Z"));
        doReturn(deviceConfiguration).when(deviceManager).getDeviceConfiguration(userId, unitId);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/devices/{unitId}/configuration?userId={userId}", unitId, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("id").value(deviceConfiguration.getId()))
                .andExpect(jsonPath("unitId").value(unitId))
                .andExpect(jsonPath("userId").value(userId))
                .andExpect(jsonPath("creationDate").value(sdf.format(deviceConfiguration.getCreationDate())));
    }

    @Test
    public void getDeviceConfiguration_whenFails_shouldReturnSameError() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        doThrow(AnyBarracksClientException.from(HttpStatus.INTERNAL_SERVER_ERROR)).when(deviceManager).getDeviceConfiguration(userId, unitId);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/devices/{unitId}/configuration?userId={userId}", unitId, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        result.andExpect(status().isInternalServerError());
    }

    @Test
    public void setDeviceConfiguration_whenSucceeds_shouldReturnTheCreatedConfiguration() throws Exception {
        // Given
        final JSONObject jsonObject = getJsonFromResource("setDeviceConfiguration-request");
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceConfiguration newConfiguration = DeviceConfiguration.builder()
                .build();
        final DeviceConfiguration savedConfiguration = DeviceConfiguration.builder()
                .unitId(unitId)
                .userId(userId)
                .id(UUID.randomUUID().toString())
                .creationDate(new Date(123456789000L))
                .build();
        final SimpleDateFormat sdf = new SimpleDateFormat(DeviceConfiguration.DATE_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("Z"));
        doReturn(savedConfiguration).when(deviceManager).saveDeviceConfiguration(userId, unitId, newConfiguration);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.post("/devices/{unitId}/configuration?userId={userId}", unitId, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonObject.toJSONString())
        );

        // Then
        verify(deviceManager).saveDeviceConfiguration(userId, unitId, newConfiguration);
        result.andExpect(status().isOk())
                .andExpect(jsonPath("id").value(savedConfiguration.getId()))
                .andExpect(jsonPath("userId").value(savedConfiguration.getUserId()))
                .andExpect(jsonPath("unitId").value(savedConfiguration.getUnitId()))
                .andExpect(jsonPath("creationDate").value(sdf.format(savedConfiguration.getCreationDate())));
    }

    @Test
    public void getDeviceEventsByUnitId_withoutChangedParam_shouldCallManagerWithDefaultChangedParam() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final boolean changed = true;
        doReturn(new PageImpl<DeviceEvent>(Collections.emptyList())).when(deviceManager).getDeviceEventsByUnitId(eq(userId), eq(unitId), eq(changed), any());

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/devices/{unitId}/events?userId={userId}", unitId, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(deviceManager).getDeviceEventsByUnitId(eq(userId), eq(unitId), eq(changed), any());
        result.andExpect(status().isOk());
    }

    @Test
    public void getDeviceEventsByUnitId_withTrueChangedParam_shouldCallManagerWithDefaultChangedParam() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final boolean changed = true;
        doReturn(new PageImpl<DeviceEvent>(Collections.emptyList())).when(deviceManager).getDeviceEventsByUnitId(eq(userId), eq(unitId), eq(changed), any());

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/devices/{unitId}/events?userId={userId}&onlyChanged={changed}", unitId, userId, changed)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(deviceManager).getDeviceEventsByUnitId(eq(userId), eq(unitId), eq(changed), any());
        result.andExpect(status().isOk());
    }

    @Test
    public void getDeviceEventsByUnitId_withFalseChangedParam_shouldCallManagerWithDefaultChangedParam() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final boolean changed = false;
        doReturn(new PageImpl<DeviceEvent>(Collections.emptyList())).when(deviceManager).getDeviceEventsByUnitId(eq(userId), eq(unitId), eq(changed), any());

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/devices/{unitId}/events?userId={userId}&onlyChanged={changed}", unitId, userId, changed)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(deviceManager).getDeviceEventsByUnitId(eq(userId), eq(unitId), eq(changed), any());
        result.andExpect(status().isOk());
    }

    @Test
    public void getUnit_shouldCallManager_andReturnUnit() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceConfiguration configuration = DeviceConfiguration.builder()
                .build();
        final DeviceEvent event = DeviceEvent.builder()
                .userId(userId)
                .unitId(unitId)
                .versionId(UUID.randomUUID().toString())
                .receptionDate(new Date(1234567890L))
                .build();
        final Device device = Device.builder()
                .unitId(unitId)
                .userId(userId)
                .configuration(configuration)
                .lastEvent(event)
                .build();
        doReturn(device).when(deviceManager).getDeviceByUserIdAndUnitId(userId, unitId);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/devices/{unitId}?userId={userId}", unitId, userId)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(deviceManager).getDeviceByUserIdAndUnitId(userId, unitId);
        result.andExpect(status().isOk())
                .andExpect(jsonPath("userId").value(userId))
                .andExpect(jsonPath("unitId").value(unitId))
                .andExpect(jsonPath("lastEvent.versionId").value(event.getVersionId()));
    }
}
