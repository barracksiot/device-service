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
import io.barracks.commons.util.Endpoint;
import io.barracks.deviceservice.manager.FilterManager;
import io.barracks.deviceservice.model.DeviceRequest;
import io.barracks.deviceservice.rest.MatchResource;
import io.barracks.deviceservice.utils.DeviceRequestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@BarracksResourceTest(controllers = MatchResource.class, outputDir = "build/generated-snippets/match")
public class MatchResourceConfigurationTest {

    private static final String baseUrl = "https://not.barracks.io";

    private static final Endpoint MATCH_FILTER_ENDPOINT = Endpoint.from(HttpMethod.POST, "/owners/{userId}/devices/{unitId}/match");
    private static final Endpoint MATCH_FILTER_WITH_FIRST_ENDPOINT = Endpoint.from(HttpMethod.POST, "/owners/{userId}/devices/{unitId}/match", "first={first}");

    @SpyBean
    private MatchResource resource;

    @MockBean
    private FilterManager filterManager;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void documentMatchAllFilters() throws Exception {
        // Given
        final Endpoint endpoint = MATCH_FILTER_WITH_FIRST_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceRequest request = DeviceRequestUtils.getDeviceRequest();
        final List<String> names = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        doReturn(names).when(resource).matchEvent(userId, unitId, names, false, request);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders
                        .request(
                                endpoint.getMethod(),
                                baseUrl + endpoint.getPath() + "?" + endpoint.getQuery() + "&filter={filter}&filter={filter}",
                                userId,
                                unitId,
                                false,
                                names.get(0),
                                names.get(1)
                        )
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
        );

        // Then
        verify(resource).matchEvent(userId, unitId, names, false, request);
        result.andExpect(status().isOk())
                .andDo(
                        document(
                                "all",
                                pathParameters(
                                        parameterWithName("userId").description("ID of the user"),
                                        parameterWithName("unitId").description("Unique ID of the device")
                                ),
                                requestParameters(
                                        parameterWithName("first").description("Set to false to find all matching filters"),
                                        parameterWithName("filter").description("The list of filters to match the event with")
                                )
                        )
                );
    }

    @Test
    public void documentMatchFirstFilter() throws Exception {
        // Given
        final Endpoint endpoint = MATCH_FILTER_WITH_FIRST_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceRequest request = DeviceRequestUtils.getDeviceRequest();
        final List<String> names = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        doReturn(names.get(0)).when(resource).matchEvent(userId, unitId, names, true, request);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders
                        .request(
                                endpoint.getMethod(),
                                baseUrl + endpoint.getPath() + "?" + endpoint.getQuery() + "&filter={filter}&filter={filter}",
                                userId,
                                unitId,
                                true,
                                names.get(0),
                                names.get(1)
                        )
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
        );

        // Then
        verify(resource).matchEvent(userId, unitId, names, true, request);
        result.andExpect(status().isOk())
                .andDo(
                        document(
                                "first",
                                pathParameters(
                                        parameterWithName("userId").description("ID of the user"),
                                        parameterWithName("unitId").description("Unique ID of the device")
                                ),
                                requestParameters(
                                        parameterWithName("first").description("Set to true to find only the first matching filters"),
                                        parameterWithName("filter").description("The list of filters to match the event with")
                                )
                        )
                );
    }

    @Test
    public void postRequest_withoutFirst_shouldCallResourceWithEventAndTrue() throws Exception {
        // Given
        final Endpoint endpoint = MATCH_FILTER_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceRequest request = DeviceRequestUtils.getDeviceRequest();
        final List<String> names = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>() {{
            put("filter", names);
        }};
        doReturn(null).when(resource).matchEvent(userId, unitId, names, true, request);

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).queryParams(params).getURI(userId, unitId))
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
        );

        // Then
        verify(resource).matchEvent(userId, unitId, names, true, request);
        result.andExpect(status().isOk());
    }

    @Test
    public void postRequest_withFirstTrue_shouldCallResourceWithEventAndTrue() throws Exception {
        // Given
        final Endpoint endpoint = MATCH_FILTER_WITH_FIRST_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceRequest request = DeviceRequestUtils.getDeviceRequest();
        final List<String> names = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>() {{
            put("filter", names);
        }};
        doReturn(null).when(resource).matchEvent(userId, unitId, names, true, request);

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).queryParams(params).getURI(userId, unitId, "true"))
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
        );

        // Then
        verify(resource).matchEvent(userId, unitId, names, true, request);
        result.andExpect(status().isOk());
    }

    @Test
    public void postRequest_withFirstFalse_shouldCallResourceWithEventAndFalse() throws Exception {
        // Given
        final Endpoint endpoint = MATCH_FILTER_WITH_FIRST_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final DeviceRequest request = DeviceRequestUtils.getDeviceRequest();
        final List<String> names = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>() {{
            put("filter", names);
        }};
        doReturn(null).when(resource).matchEvent(userId, unitId, names, false, request);

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).queryParams(params).getURI(userId, unitId, "false"))
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
        );

        // Then
        verify(resource).matchEvent(userId, unitId, names, false, request);
        result.andExpect(status().isOk());
    }

    @Test
    public void postRequest_withoutFilter_shouldReturnBadRequest() throws Exception {
        // Given
        final Endpoint endpoint = MATCH_FILTER_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();

        // When
        final ResultActions result = mvc.perform(request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI(userId, unitId))
                .content("{}")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        );

        // Then
        verifyZeroInteractions(resource);
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void postEvent_withNoEvent_shouldReturnUnProcessableEntity() throws Exception {
        // Given
        final Endpoint endpoint = MATCH_FILTER_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final List<String> names = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>() {{
            put("filter", names);
        }};

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).queryParams(params).getURI(userId, unitId))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
        );

        // Then
        verifyZeroInteractions(resource);
        result.andExpect(status().isUnprocessableEntity());
    }
}
