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
import io.barracks.commons.test.PagedResourcesUtils;
import io.barracks.commons.util.Endpoint;
import io.barracks.deviceservice.model.Filter;
import io.barracks.deviceservice.model.operator.Operator;
import io.barracks.deviceservice.rest.FilterResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.FileCopyUtils;

import java.util.Arrays;
import java.util.UUID;

import static io.barracks.deviceservice.utils.FilterUtils.getFilter;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@BarracksResourceTest(controllers = FilterResource.class, outputDir = "build/generated-snippets/filters")
public class FilterResourceConfigurationTest {

    private static final String baseUrl = "https://not.barracks.io";

    private static final Endpoint CREATE_FILTER_ENDPOINT = Endpoint.from(HttpMethod.POST, "/owners/{userId}/filters");
    private static final Endpoint UPDATE_FILTER_ENDPOINT = Endpoint.from(HttpMethod.POST, "/owners/{userId}/filters/{name}");
    private static final Endpoint GET_FILTERS_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/filters");
    private static final Endpoint GET_FILTER_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/filters/{name}");
    private static final Endpoint DELETE_FILTER_ENDPOINT = Endpoint.from(HttpMethod.DELETE, "/owners/{userId}/filters/{name}");

    @MockBean
    private FilterResource resource;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Value("classpath:io/barracks/deviceservice/filter.json")
    private Resource filter;

    @Value("classpath:io/barracks/deviceservice/filter-documentation.json")
    private Resource filterDoc;

    @Value("classpath:io/barracks/deviceservice/operator-documentation.json")
    private Resource operatorDoc;


    private PagedResourcesAssembler<Filter> assembler = PagedResourcesUtils.getPagedResourcesAssembler();

    @Test
    public void documentCreateFilter() throws Exception {
        // Given
        final Endpoint endpoint = CREATE_FILTER_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final Filter request = mapper.readValue(filterDoc.getInputStream(), Filter.class);
        final Filter response = getFilter();
        doReturn(response).when(resource).createFilter(userId, request);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders
                        .request(
                                endpoint.getMethod(),
                                baseUrl + endpoint.getPath(),
                                userId
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FileCopyUtils.copyToByteArray(filterDoc.getInputStream()))
        );

        // Then
        verify(resource).createFilter(userId, request);
        result.andExpect(status().isCreated())
                .andDo(
                        document(
                                "create",
                                pathParameters(
                                        parameterWithName("userId").description("ID of the user")
                                ),
                                requestFields(
                                        fieldWithPath("name").description("The name for the filter"),
                                        fieldWithPath("query").description("The query used to define devices filtered")
                                )
                        )
                );
    }

    @Test
    public void postFilter_withValidFilter_shouldCallCreateFilterAndReturnValue() throws Exception {
        // Given
        final Endpoint endpoint = CREATE_FILTER_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final Filter request = mapper.readValue(filter.getInputStream(), Filter.class);
        final Filter response = getFilter();
        doReturn(response).when(resource).createFilter(userId, request);

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FileCopyUtils.copyToByteArray(filter.getInputStream()))
        );

        // Then
        verify(resource).createFilter(userId, request);
        result.andExpect(status().isCreated())
                .andExpect(content().string(mapper.writeValueAsString(response)));
    }

    @Test
    public void postFilter_withInvalidFilter_shouldReturnBadRequest() throws Exception {
        // Given
        final Endpoint endpoint = CREATE_FILTER_ENDPOINT;
        final String badFilter = "{}";
        final String userId = UUID.randomUUID().toString();

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badFilter)
        );

        // Then
        result.andExpect(status().isBadRequest());
        verifyZeroInteractions(resource);
    }

    @Test
    public void documentGetFilters() throws Exception {
        //Given
        final Endpoint endpoint = GET_FILTERS_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 20);
        final Page<Filter> page = new PageImpl<>(Arrays.asList(
                getFilter(),
                getFilter()
        ));
        final PagedResources expected = assembler.toResource(page);
        doReturn(expected).when(resource).getFilters(userId, pageable);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders
                        .request(
                                endpoint.getMethod(), baseUrl + endpoint.getPath(),
                                userId
                        )
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        );

        //Then
        verify(resource).getFilters(userId, pageable);
        result.andExpect(status().isOk())
                .andDo(
                        document(
                                "list",
                                pathParameters(
                                        parameterWithName("userId").description("ID of the user")
                                )
                        )
                );
    }

    @Test
    public void getFilters_shouldCallResourceAndReturnFilterList() throws Exception {
        //Given
        final Endpoint endpoint = GET_FILTERS_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Page<Filter> page = new PageImpl<>(Arrays.asList(
                getFilter(),
                getFilter()
        ));
        final PagedResources expected = assembler.toResource(page);
        doReturn(expected).when(resource).getFilters(userId, pageable);

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).pageable(pageable).getURI(userId))
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        );

        //Then
        verify(resource).getFilters(userId, pageable);
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.filters", hasSize(page.getNumberOfElements())));
    }

    @Test
    public void documentGetFitler() throws Exception {
        // Given
        final Endpoint endpoint = GET_FILTER_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String name = UUID.randomUUID().toString();
        final Filter expected = getFilter();
        doReturn(expected).when(resource).getFilter(userId, name);

        // When
        final ResultActions result = mvc.perform(RestDocumentationRequestBuilders.request(
                endpoint.getMethod(),
                baseUrl + endpoint.getPath(),
                userId, name
        ));

        // Then
        result.andExpect(status().isOk())
                .andDo(
                        document(
                                "get",
                                pathParameters(
                                        parameterWithName("userId").description("ID of the user"),
                                        parameterWithName("name").description("The name given to the filter")
                                )
                        )
                );
    }

    @Test
    public void getFilter_shouldCallResourceAndReturnFilter() throws Exception {
        // Given
        final Endpoint endpoint = GET_FILTER_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String name = UUID.randomUUID().toString();
        final Filter expected = getFilter();
        doReturn(expected).when(resource).getFilter(userId, name);

        // When
        final ResultActions result = mvc.perform(request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI(userId, name)));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(expected)));
    }

    @Test
    public void documentDeleteFilter() throws Exception {
        // Given
        final Endpoint endpoint = DELETE_FILTER_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String filterName = UUID.randomUUID().toString();

        // When
        final ResultActions result = mvc.perform(RestDocumentationRequestBuilders.request(
                endpoint.getMethod(),
                baseUrl + endpoint.getPath(),
                userId, filterName
        ));

        // Then
        verify(resource).deleteFilter(userId, filterName);
        result.andExpect(status().isNoContent())
                .andDo(
                        document(
                                "delete",
                                pathParameters(
                                        parameterWithName("userId").description("ID of the user"),
                                        parameterWithName("name").description("The name given to the filter")
                                )
                        )
                );
    }

    @Test
    public void deleteFilter_shouldCallResourceAndReturnNoContent() throws Exception {
        // Given
        final Endpoint endpoint = DELETE_FILTER_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String filterName = UUID.randomUUID().toString();

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI(userId, filterName))
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        );

        // Then
        verify(resource).deleteFilter(userId, filterName);
        result.andExpect(status().isNoContent());
    }

    @Test
    public void documentUpdateFilter() throws Exception {
        // Given
        final Endpoint endpoint = UPDATE_FILTER_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String name = UUID.randomUUID().toString();
        final Operator request = mapper.readValue(operatorDoc.getInputStream(), Operator.class);
        final Filter response = getFilter();
        doReturn(response).when(resource).updateFilter(userId, name, request);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders
                        .request(
                                endpoint.getMethod(),
                                baseUrl + endpoint.getPath(),
                                userId,
                                name
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FileCopyUtils.copyToByteArray(operatorDoc.getInputStream()))
        );

        // Then
        verify(resource).updateFilter(userId, name, request);
        result.andExpect(status().isOk())
                .andDo(
                        document(
                                "update",
                                pathParameters(
                                        parameterWithName("userId").description("ID of the user"),
                                        parameterWithName("name").description("The name for the filter")
                                ),
                                requestFields(
                                        fieldWithPath("eq").description("The query used to define devices filtered")
                                )
                        )
                );
    }

    @Test
    public void updateFilter_withValidFilter_shouldCallUpdateFilterAndReturnValue() throws Exception {
        // Given
        final Endpoint endpoint = UPDATE_FILTER_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String name = UUID.randomUUID().toString();
        final Operator request = mapper.readValue(operatorDoc.getInputStream(), Operator.class);
        final Filter response = getFilter();
        doReturn(response).when(resource).updateFilter(userId, name, request);

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI(userId, name))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FileCopyUtils.copyToByteArray(operatorDoc.getInputStream()))
        );

        // Then
        verify(resource).updateFilter(userId, name, request);
        result.andExpect(status().isOk())
                .andExpect(content().string(mapper.writeValueAsString(response)));
    }

}