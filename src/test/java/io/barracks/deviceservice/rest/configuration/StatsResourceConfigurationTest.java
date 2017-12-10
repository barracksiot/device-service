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
import io.barracks.deviceservice.model.DataSet;
import io.barracks.deviceservice.rest.StatsResource;
import io.barracks.deviceservice.rest.entity.DateRange;
import io.barracks.deviceservice.utils.DataSetUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@BarracksResourceTest(controllers = StatsResource.class, outputDir = "build/generated-snippets/stats")
public class StatsResourceConfigurationTest {
    private static final Endpoint GET_ALIVE_DEVICES_COUNT_ENDPOINT = Endpoint.from(HttpMethod.GET, "/stats/{userId}/devices/lastSeen", "start={start}&end={end}");
    private static final Endpoint GET_UNIQUE_DEVICES_COUNT_ENDPOINT = Endpoint.from(HttpMethod.GET, "/stats/{userId}/devices/seen", "start={start}&end={end}");
    private static final Endpoint GET_DEVICE_COUNT_PER_USER_ID_ENDPOINT = Endpoint.from(HttpMethod.GET, "/stats/devices/perUserId");

    private static final String baseUrl = "https://not.barracks.io";

    @MockBean
    private StatsResource resource;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MockMvc mvc;

    @Test
    public void documentGetLastSeen() throws Exception {
        // Given
        final Endpoint endpoint = GET_ALIVE_DEVICES_COUNT_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final OffsetDateTime start = OffsetDateTime.now().minusSeconds(42L);
        final OffsetDateTime end = OffsetDateTime.now().plusSeconds(42L);
        final DataSet expected = DataSet.builder().total(BigDecimal.valueOf(42L)).build();
        doReturn(expected).when(resource).getAliveDevicesCount(userId, new DateRange(start, end));

        // When
        final ResultActions result = mvc.perform(RestDocumentationRequestBuilders.request(
                endpoint.getMethod(),
                baseUrl + endpoint.getPath() + "?" + endpoint.getQuery(),
                userId, start, end
        ));

        // Then
        verify(resource).getAliveDevicesCount(userId, new DateRange(start, end));
        result.andExpect(status().isOk())
                .andDo(
                        document(
                                "last-seen",
                                requestParameters(
                                        parameterWithName("start").description("Start date for the request"),
                                        parameterWithName("end").description("End date for the request")
                                ),
                                responseFields(
                                        fieldWithPath("total").description("The total amount of devices whose latest request was seen between start and end")
                                )
                        )
                );
    }

    @Test
    public void getLastSeen_shouldCallResource_andReturnResult() throws Exception {
        // Given
        final Endpoint endpoint = GET_ALIVE_DEVICES_COUNT_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final OffsetDateTime start = OffsetDateTime.now().minusSeconds(42L);
        final OffsetDateTime end = OffsetDateTime.now().plusSeconds(42L);
        final DataSet expected = DataSetUtils.getDataSet();
        doReturn(expected).when(resource).getAliveDevicesCount(userId, new DateRange(start, end));

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI(userId, start, end))
        );

        // Then
        verify(resource).getAliveDevicesCount(userId, new DateRange(start, end));
        result.andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(expected)));
    }

    @Test
    public void documentGetSeen() throws Exception {
        // Given
        final Endpoint endpoint = GET_UNIQUE_DEVICES_COUNT_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final OffsetDateTime start = OffsetDateTime.now().minusSeconds(42L);
        final OffsetDateTime end = OffsetDateTime.now().plusSeconds(42L);
        final DataSet expected = DataSet.builder().total(BigDecimal.valueOf(42L)).build();
        doReturn(expected).when(resource).getUniqueDevicesCount(userId, new DateRange(start, end));

        // When
        final ResultActions result = mvc.perform(RestDocumentationRequestBuilders.request(
                endpoint.getMethod(),
                baseUrl + endpoint.getPath() + "?" + endpoint.getQuery(),
                userId, start, end
        ));

        // Then
        verify(resource).getUniqueDevicesCount(userId, new DateRange(start, end));
        result.andExpect(status().isOk())
                .andDo(
                        document(
                                "seen",
                                requestParameters(
                                        parameterWithName("start").description("Start date for the request"),
                                        parameterWithName("end").description("End date for the request")
                                ),
                                responseFields(
                                        fieldWithPath("total").description("The total amount of devices having sent a request between start and end")
                                )
                        )
                );
    }

    @Test
    public void getSeenDevices_shouldCallResource_andReturnResult() throws Exception {
        // Given
        final Endpoint endpoint = GET_UNIQUE_DEVICES_COUNT_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final OffsetDateTime start = OffsetDateTime.now().minusSeconds(42L);
        final OffsetDateTime end = OffsetDateTime.now().plusSeconds(42L);
        final DataSet expected = DataSetUtils.getDataSet();
        doReturn(expected).when(resource).getUniqueDevicesCount(userId, new DateRange(start, end));

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI(userId, start, end))
        );

        // Then
        verify(resource).getUniqueDevicesCount(userId, new DateRange(start, end));
        result.andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(expected)));
    }

    @Test
    public void documentGetDevicesPerUserId() throws Exception {
        // Given
        final Endpoint endpoint = GET_DEVICE_COUNT_PER_USER_ID_ENDPOINT;
        final DataSet expected = DataSetUtils.getDataSet();
        doReturn(expected).when(resource).getDeviceCountPerUserId();

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI())
        );

        // Then
        verify(resource).getDeviceCountPerUserId();
        result.andExpect(status().isOk()).andDo(
                document(
                        "per-user-id",
                        responseFields(
                                fieldWithPath("values").description("The total amount of devices for each user"),
                                fieldWithPath("total").description("The total amount of devices in the system")
                        )
                )
        );
    }

    @Test
    public void getDevicesPerUserId_shouldCallResource_andReturnResult() throws Exception {
        // Given
        final Endpoint endpoint = GET_DEVICE_COUNT_PER_USER_ID_ENDPOINT;
        final DataSet expected = DataSetUtils.getDataSet();
        doReturn(expected).when(resource).getDeviceCountPerUserId();

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI())
        );

        // Then
        verify(resource).getDeviceCountPerUserId();
        result.andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(expected)));
    }
}
