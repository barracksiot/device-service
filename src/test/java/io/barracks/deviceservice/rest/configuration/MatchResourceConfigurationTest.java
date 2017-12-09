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
import io.barracks.commons.test.ServiceClientTest;
import io.barracks.deviceservice.config.ExceptionConfig;
import io.barracks.deviceservice.model.DeviceComponentRequest;
import io.barracks.deviceservice.rest.MatchResource;
import io.barracks.deviceservice.utils.DeviceComponentRequestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class MatchResourceConfigurationTest extends ServiceClientTest {
    @Rule
    public final RestDocumentation restDocumentation = new RestDocumentation("build/generated-snippets");
    private MatchResource resource;
    private MockMvc mvc;
    private ObjectMapper objectMapper = new ObjectMapper();
    private HateoasPageableHandlerMethodArgumentResolver argumentResolver = new HateoasPageableHandlerMethodArgumentResolver();

    @Before
    public void setUp() throws Exception {
        resource = mock(MatchResource.class);
        final RestDocumentationResultHandler document = document("{method-name}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()));
        this.mvc = MockMvcBuilders
                .standaloneSetup(resource)
                .setHandlerExceptionResolvers(new ExceptionConfig().restExceptionResolver().build())
                .setCustomArgumentResolvers(argumentResolver)
                .apply(documentationConfiguration(restDocumentation))
                .alwaysDo(document)
                .build();
        reset(resource);
    }

    @Test
    public void postEvent_withoutFirst_shouldCallResourceWithEventAndTrue() throws Exception {
        // Given
        final DeviceComponentRequest request = DeviceComponentRequestUtils.getDeviceComponentRequest();
        final List<String> names = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        doReturn(null).when(resource).matchEvent(names, true, request);

        // When
        final ResultActions result = mvc.perform(MockMvcRequestBuilders.post("/match?filter={filter}&filter={filter}", names.get(0), names.get(1))
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        );

        // Then
        verify(resource).matchEvent(names, true, request);
        result.andExpect(status().isOk());
    }

    @Test
    public void postEvent_withFirstTrue_shouldCallResourceWithEventAndTrue() throws Exception {
        // Given
        final DeviceComponentRequest request = DeviceComponentRequestUtils.getDeviceComponentRequest();
        final List<String> names = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        doReturn(null).when(resource).matchEvent(names, true, request);

        // When
        final ResultActions result = mvc.perform(MockMvcRequestBuilders.post("/match?first=true&filter={filter}&filter={filter}", names.get(0), names.get(1))
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        );

        // Then
        verify(resource).matchEvent(names, true, request);
        result.andExpect(status().isOk());
    }

    @Test
    public void postEvent_withFirstFalse_shouldCallResourceWithEventAndFalse() throws Exception {
        // Given
        final DeviceComponentRequest request = DeviceComponentRequestUtils.getDeviceComponentRequest();
        final List<String> names = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        doReturn(null).when(resource).matchEvent(names, false, request);

        // When
        final ResultActions result = mvc.perform(MockMvcRequestBuilders.post("/match?first=false&filter={filter}&filter={filter}", names.get(0), names.get(1))
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        );

        // Then
        verify(resource).matchEvent(names, false, request);
        result.andExpect(status().isOk());
    }

    @Test
    public void postEvent_withInvalidEvent_shouldReturnBadRequest() throws Exception {
        // Given
        final List<String> names = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());

        // When
        final ResultActions result = mvc.perform(MockMvcRequestBuilders.post("/match?filter={filter}&filter={filter}", names.get(0), names.get(1))
                .content("{}")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        );

        // Then
        verifyZeroInteractions(resource);
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void postEvent_withoutFilter_shouldReturnBadRequest() throws Exception {
        // Given

        // When
        final ResultActions result = mvc.perform(MockMvcRequestBuilders.post("/match")
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
        final List<String> names = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());

        // When
        final ResultActions result = mvc.perform(MockMvcRequestBuilders.post("/match?filter={filter}&filter={filter}", names.get(0), names.get(1))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        );

        // Then
        verifyZeroInteractions(resource);
        result.andExpect(status().isUnprocessableEntity());
    }
}
