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
import io.barracks.deviceservice.model.Filter;
import io.barracks.deviceservice.rest.FilterResource;
import io.barracks.deviceservice.utils.FilterUtils;
import io.barracks.deviceservice.utils.PagedResourcesUtils;
import net.minidev.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class FilterResourceConfigurationTest extends ServiceClientTest {
    @Rule
    public final RestDocumentation restDocumentation = new RestDocumentation("build/generated-snippets");
    private FilterResource filterResource;
    private MockMvc mvc;
    private ObjectMapper objectMapper = new ObjectMapper();
    private HateoasPageableHandlerMethodArgumentResolver argumentResolver = new HateoasPageableHandlerMethodArgumentResolver();

    @Before
    public void setUp() throws Exception {
        filterResource = mock(FilterResource.class);
        final RestDocumentationResultHandler document = document("{method-name}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()));
        this.mvc = MockMvcBuilders
                .standaloneSetup(filterResource)
                .setHandlerExceptionResolvers(new ExceptionConfig().restExceptionResolver().build())
                .setCustomArgumentResolvers(argumentResolver)
                .apply(documentationConfiguration(restDocumentation))
                .alwaysDo(document)
                .build();
        reset(filterResource);
    }

    @Test
    public void postFilter_withValidFilter_shouldCreateFilterAndReturnValue() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final JSONObject jsonRequest = getJsonFromResource("valid");
        final Filter request = objectMapper.readValue(jsonRequest.toJSONString(), Filter.class);
        final Filter response = FilterUtils.getFilter();
        final String expected = objectMapper.writeValueAsString(response);
        doReturn(response).when(filterResource).createFilter(userId, request);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.post("/owners/{userId}/filters", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest.toJSONString())
        );

        // Then
        verify(filterResource).createFilter(userId, request);
        result.andExpect(status().isCreated())
                .andExpect(content().string(expected));
    }

    @Test
    public void postFilter_withInvalidFilter_shouldReturnBadRequest() throws Exception {
        // Given
        final String badFilter = "{}";
        final String userId = UUID.randomUUID().toString();

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.post("/owners/{userId}/filters", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badFilter)
        );

        // Then
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void getFilters_whenAllIsFine_shouldCallResourceAndReturnFilterList() throws Exception {
        //Given
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Filter filter1 = FilterUtils.getFilter();
        final Filter filter2 = FilterUtils.getFilter();
        final Page<Filter> page = new PageImpl<>(Arrays.asList(filter1, filter2));
        final PagedResources<Resource<Filter>> expected = PagedResourcesUtils.buildPagedResourcesFromPage(page);
        doReturn(expected).when(filterResource).getFilters(userId, pageable);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/owners/{userId}/filters", userId)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .params(queryFrom(pageable))
        );

        //Then
        verify(filterResource).getFilters(userId, pageable);
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(page.getNumberOfElements())))
                .andExpect(jsonPath("$.content[0].name").value(filter1.getName()))
                .andExpect(jsonPath("$.content[1].name").value(filter2.getName()));
    }

    @Test
    public void deleteFilter_whenNoFilter_shouldCallResourceAndReturnNoContent() throws Exception {

        // Given
        final String userId = UUID.randomUUID().toString();
        final String filterName = UUID.randomUUID().toString();

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.delete("/owners/{userId}/filters/{name}", userId, filterName)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        );

        // Then
        verify(filterResource).deleteFilter(userId, filterName);
        result.andExpect(status().isNoContent());
    }
}