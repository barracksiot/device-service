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
import io.barracks.commons.test.ServiceClientTest;
import io.barracks.deviceservice.config.ExceptionConfig;
import io.barracks.deviceservice.manager.SegmentManager;
import io.barracks.deviceservice.model.Device;
import io.barracks.deviceservice.model.Segment;
import io.barracks.deviceservice.utils.DeviceUtils;
import net.minidev.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.MultiValueMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class SegmentResourceTest extends ServiceClientTest {
    @Rule
    public final RestDocumentation restDocumentation = new RestDocumentation("build/generated-snippets");
    private SegmentResource segmentResource;
    private MockMvc mvc;

    @Mock
    private SegmentManager segmentManager;

    private HateoasPageableHandlerMethodArgumentResolver argumentResolver = new HateoasPageableHandlerMethodArgumentResolver();

    @Before
    public void setUp() throws Exception {
        final PagedResourcesAssembler<Segment> assembler = new PagedResourcesAssembler<>(argumentResolver, null);
        final PagedResourcesAssembler<Device> deviceAssembler = new PagedResourcesAssembler<>(argumentResolver, null);
        segmentResource = new SegmentResource(assembler, deviceAssembler, segmentManager);
        final RestDocumentationResultHandler document = document("{method-name}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()));
        this.mvc = MockMvcBuilders
                .standaloneSetup(segmentResource)
                .setHandlerExceptionResolvers(new ExceptionConfig().restExceptionResolver().build())
                .setCustomArgumentResolvers(argumentResolver)
                .apply(documentationConfiguration(restDocumentation))
                .alwaysDo(document)
                .build();
        reset(segmentManager);
    }

    @Test
    public void createSegment_shouldCallManager_andReturnSegment() throws Exception {
        // Given
        final JSONObject json = getJsonFromResource("request");
        final Segment segment = new ObjectMapper().readValue(json.toJSONString(), Segment.class);
        final String segmentId = UUID.randomUUID().toString();
        final Segment expected = Segment.builder()
                .id(segmentId)
                .userId(segment.getUserId())
                .name(segment.getName())
                .query(segment.getQuery())
                .build();
        doReturn(expected).when(segmentManager).createSegment(segment);

        // When
        ResultActions result = mvc.perform(
                MockMvcRequestBuilders.post("/segments")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.toJSONString())
        );

        // Then
        verify(segmentManager).createSegment(segment);
        result.andExpect(status().isOk()).andExpect(jsonPath("id").value(expected.getId()));
    }

    @Test
    public void createSegment_withInvalidQuery_shouldThrowException() throws Exception {
        // Given
        final JSONObject json = getJsonFromResource("invalidRequest");

        // When
        ResultActions result = mvc.perform(
                MockMvcRequestBuilders.post("/segments")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.toJSONString())
        );

        // Then
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void createSegment_withNoQuery_shouldThrowException() throws Exception {
        // Given
        final JSONObject json = getJsonFromResource("request");
        json.remove("query");

        // When
        ResultActions result = mvc.perform(
                MockMvcRequestBuilders.post("/segments")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.toJSONString())
        );

        // Then
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void createSegment_withNoName_shouldThrowException() throws Exception {
        // Given
        final JSONObject json = getJsonFromResource("request");
        json.remove("name");

        // When
        ResultActions result = mvc.perform(
                MockMvcRequestBuilders.post("/segments")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.toJSONString())
        );

        // Then
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void createSegment_withNoUserId_shouldThrowException() throws Exception {
        // Given
        final JSONObject json = getJsonFromResource("request");
        json.remove("userId");

        // When
        ResultActions result = mvc.perform(
                MockMvcRequestBuilders.post("/segments")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.toJSONString())
        );

        // Then
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void getSegmentById_shouldCallManager_andReturnSegment() throws Exception {
        // Given
        final String segmentId = UUID.randomUUID().toString();
        final JSONObject json = getJsonFromResource("request");
        final Segment segment = new ObjectMapper().readValue(json.toJSONString(), Segment.class);
        json.put("id", segmentId);
        final Segment response = segment.toBuilder().id(segmentId).build();
        doReturn(response).when(segmentManager).getSegmentById(segmentId);

        // When
        final ResultActions result = mvc.perform(MockMvcRequestBuilders
                .get("/segments/" + segmentId)
                .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(segmentManager).getSegmentById(segmentId);
        result.andExpect(status().isOk()).andExpect(jsonPath("id").value(segmentId));
    }

    @Test
    public void updateSegment_shouldCallManager_andReturnSegment() throws Exception {
        // Given
        final String segmentId = UUID.randomUUID().toString();
        final JSONObject json = getJsonFromResource("request");
        final Segment update = new ObjectMapper().readValue(json.toJSONString(), Segment.class);
        final Segment response = update.toBuilder().id(segmentId).build();
        doReturn(response).when(segmentManager).updateSegment(segmentId, update);

        // When
        final ResultActions result = mvc.perform(MockMvcRequestBuilders
                .put("/segments/" + segmentId)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.toJSONString())
        );

        // Then
        verify(segmentManager).updateSegment(segmentId, update);
        result.andExpect(status().isOk()).andExpect(jsonPath("id").value(segmentId));
    }

    @Test
    public void updateSegment_withNoQuery_shouldThrowException() throws Exception {
        // Given
        final String segmentId = UUID.randomUUID().toString();
        final JSONObject json = getJsonFromResource("request");
        json.remove("query");

        // When
        ResultActions result = mvc.perform(
                MockMvcRequestBuilders.put("/segments/" + segmentId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.toJSONString())
        );

        // Then
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void updateSegment_withNoName_shouldThrowException() throws Exception {
        // Given
        final String segmentId = UUID.randomUUID().toString();
        final JSONObject json = getJsonFromResource("request");
        json.remove("name");

        // When
        ResultActions result = mvc.perform(
                MockMvcRequestBuilders.put("/segments/" + segmentId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.toJSONString())
        );

        // Then
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void updateSegment_withNoUserId_shouldThrowException() throws Exception {
        // Given
        final String segmentId = UUID.randomUUID().toString();
        final JSONObject json = getJsonFromResource("request");
        json.remove("userId");

        // When
        ResultActions result = mvc.perform(
                MockMvcRequestBuilders.put("/segments/" + segmentId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.toJSONString())
        );

        // Then
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void getSegmentsByUserIdAndStatusActiveShouldCallManagerAndReturnResult() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final List<Segment> segments = Collections.singletonList(Segment.builder().userId(userId).build());
        doReturn(segments).when(segmentManager).getActiveSegments(userId);

        // When
        ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/segments")
                        .param("userId", userId)
                        .param("status", "active")
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(segmentManager).getActiveSegments(userId);
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].userId").value(userId));
    }

    @Test
    public void getSegmentsByUserIdAndStatusInactiveShouldCallManagerAndReturnResult() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final List<Segment> segments = Collections.singletonList(Segment.builder().userId(userId).build());
        doReturn(segments).when(segmentManager).getInactiveSegments(userId);

        // When
        ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/segments")
                        .param("userId", userId)
                        .param("status", "inactive")
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(segmentManager).getInactiveSegments(userId);
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].userId").value(userId));
    }

    @Test
    public void getSegmentsByStatus_whenStatusUnknown_shouldReturnError() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String status = "kjdhfkjdhsfakhjf";

        // When
        ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/segments")
                        .param("userId", userId)
                        .param("status", status)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void updateOrder_shouldCallManager_andReturnResults() throws Exception {
        // Given
        final List<String> order = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        final List<String> expected = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        final String userId = UUID.randomUUID().toString();
        doReturn(expected).when(segmentManager).updateSegmentOrder(userId, order);

        // When
        ResultActions result = mvc.perform(
                MockMvcRequestBuilders.put("/segments/order")
                        .param("userId", userId)
                        .content(new ObjectMapper().writeValueAsString(order))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(segmentManager).updateSegmentOrder(userId, order);
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.[0]").value(expected.get(0)))
                .andExpect(jsonPath("$.[1]").value(expected.get(1)));
    }

    @Test
    public void getDevicesForSegment_shouldCallManager_andReturnResult() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final List<Device> devices = Arrays.asList(
                Device.builder().unitId(UUID.randomUUID().toString()).userId(userId).build(),
                Device.builder().unitId(UUID.randomUUID().toString()).userId(userId).build()
        );
        doReturn(new PageImpl<>(devices)).when(segmentManager).getDevicesBySegmentId(segmentId, pageable);

        // When
        ResultActions result = mvc.perform(MockMvcRequestBuilders
                .get(getUri(queryFrom(pageable), "segments", segmentId, "devices"))
                .accept(MediaTypes.HAL_JSON_VALUE)
        );

        // Then
        verify(segmentManager).getDevicesBySegmentId(segmentId, pageable);
        result.andExpect(jsonPath("$.content[0].unitId").value(devices.get(0).getUnitId()));
        result.andExpect(jsonPath("$.content[1].unitId").value(devices.get(1).getUnitId()));
    }

    @Test
    public void getOtherDevicesForUser_shouldCallManager_andReturnResult() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final MultiValueMap<String, String> query = queryFrom(pageable);
        query.add("userId", userId);
        final List<Device> devices = Arrays.asList(
                Device.builder().unitId(UUID.randomUUID().toString()).userId(userId).build(),
                Device.builder().unitId(UUID.randomUUID().toString()).userId(userId).build()
        );
        doReturn(new PageImpl<>(devices)).when(segmentManager).getOtherDevicesForUser(userId, pageable);

        // When
        ResultActions result = mvc.perform(MockMvcRequestBuilders
                .get(getUri(query, "segments", "other", "devices"))
                .accept(MediaTypes.HAL_JSON_VALUE)
        );

        // Then
        verify(segmentManager).getOtherDevicesForUser(userId, pageable);
        result.andExpect(jsonPath("$.content[0].unitId").value(devices.get(0).getUnitId()));
        result.andExpect(jsonPath("$.content[1].unitId").value(devices.get(1).getUnitId()));
    }

    @Test
    public void getDevicesForSegmentAndVersion_whenUsingSegmentId_shouldCallManagerAndReturnResult() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final List<Device> devices = Arrays.asList(
                DeviceUtils.getDevice(),
                DeviceUtils.getDevice()
        );
        final MultiValueMap<String, String> query = queryFrom(pageable);
        query.add("userId", userId);
        query.add("versionId", versionId);
        doReturn(new PageImpl<>(devices)).when(segmentManager).getDevicesBySegmentIdAndVersionId(segmentId, versionId, pageable);

        // When
        ResultActions result = mvc.perform(MockMvcRequestBuilders
                .get(getUri(query, "segments", segmentId, "devices"))
                .accept(MediaTypes.HAL_JSON_VALUE)
        );

        // Then
        verify(segmentManager).getDevicesBySegmentIdAndVersionId(segmentId, versionId, pageable);
        result.andExpect(jsonPath("$.content[0].unitId").value(devices.get(0).getUnitId()));
        result.andExpect(jsonPath("$.content[1].unitId").value(devices.get(1).getUnitId()));
    }

    @Test
    public void getDevicesForSegmentAndVersion_whenUsingOther_shouldCallManagerAndReturnResult() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String segmentId = "other";
        final String versionId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final List<Device> devices = Arrays.asList(
                DeviceUtils.getDevice(),
                DeviceUtils.getDevice()
        );
        final MultiValueMap<String, String> query = queryFrom(pageable);
        query.add("userId", userId);
        query.add("versionId", versionId);
        doReturn(new PageImpl<>(devices)).when(segmentManager).getOtherDevicesForUserAndVersionId(userId, versionId, pageable);

        // When
        ResultActions result = mvc.perform(MockMvcRequestBuilders
                .get(getUri(query, "segments", segmentId, "devices"))
                .accept(MediaTypes.HAL_JSON_VALUE)
        );

        // Then
        verify(segmentManager).getOtherDevicesForUserAndVersionId(userId, versionId, pageable);
        result.andExpect(jsonPath("$.content[0].unitId").value(devices.get(0).getUnitId()));
        result.andExpect(jsonPath("$.content[1].unitId").value(devices.get(1).getUnitId()));
    }
}