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

import io.barracks.commons.test.ServiceClientTest;
import io.barracks.deviceservice.config.ExceptionConfig;
import io.barracks.deviceservice.manager.StatsManager;
import io.barracks.deviceservice.model.DataSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.*;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class StatsResourceTest extends ServiceClientTest {
    @Rule
    public final RestDocumentation restDocumentation = new RestDocumentation("build/generated-snippets");
    private StatsResource statsResource;
    private MockMvc mvc;

    @Mock
    private StatsManager statsManager;

    private HateoasPageableHandlerMethodArgumentResolver argumentResolver = new HateoasPageableHandlerMethodArgumentResolver();

    @Before
    public void setUp() throws Exception {
        statsResource = new StatsResource(statsManager);
        final RestDocumentationResultHandler document = document("{method-name}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()));
        this.mvc = MockMvcBuilders
                .standaloneSetup(statsResource)
                .setHandlerExceptionResolvers(new ExceptionConfig().restExceptionResolver().build())
                .setCustomArgumentResolvers(argumentResolver)
                .apply(documentationConfiguration(restDocumentation))
                .alwaysDo(document)
                .build();
        reset(statsManager);
    }

    @Test
    public void getDeviceCountPerVersionId_shouldReturnProperlyFormattedJson() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final DataSet dataSet = prepareRandomDataSet();
        when(statsManager.getDeviceCountPerVersionId(userId)).thenReturn(dataSet);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/stats/{userId}/devices/perVersionId", userId)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(statsManager).getDeviceCountPerVersionId(userId);
        result.andExpect(DataSetMatcher.from(dataSet));
    }

    @Test
    public void getLastSeen_shouldReturnProperlyFormattedJson() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final DataSet dataSet = prepareRandomDataSet();
        when(statsManager.getLastSeenDeviceCount(userId, OffsetDateTime.MIN, OffsetDateTime.MAX)).thenReturn(dataSet);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/stats/{userId}/devices/lastSeen", userId)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(statsManager).getLastSeenDeviceCount(userId, OffsetDateTime.MIN, OffsetDateTime.MAX);
        result.andExpect(DataSetMatcher.from(dataSet));
    }

    @Test
    public void getLastSeen_withBoundaries_shouldUseCorrectDates() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final DataSet dataSet = prepareRandomDataSet();
        final String start = "2000-01-01T21:49:53.666Z";
        final String end = "2012-12-21T21:49:53.666Z";
        final OffsetDateTime startDate = OffsetDateTime.parse(start);
        final OffsetDateTime endDate = OffsetDateTime.parse(end);
        when(statsManager.getLastSeenDeviceCount(userId, startDate, endDate)).thenReturn(dataSet);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders
                        .get(
                                "/stats/{userId}/devices/lastSeen?start={start}&end={end}",
                                userId,
                                start,
                                end
                        )
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(statsManager).getLastSeenDeviceCount(userId, startDate, endDate);
        result.andExpect(DataSetMatcher.from(dataSet));
    }

    @Test
    public void getLastSeen_withIncorrectBoundaries_shouldReturnBadRequest() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String start = "2000-01-01T21:49:53.666Z";
        final String end = "2012-12-21T21:49:53.666Z";

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders
                        .get(
                                "/stats/{userId}/devices/lastSeen?start={start}&end={end}",
                                userId,
                                end,
                                start
                        )
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("type").isNotEmpty())
                .andExpect(jsonPath("detail").isNotEmpty())
                .andExpect(jsonPath("errors").isArray())
                .andExpect(jsonPath("errors").isNotEmpty());
    }

    @Test
    public void getLastSeen_withParsingError_shouldReturnBadRequest() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String start = "2000-01-01T21:49:53.666Z";
        final String end = "CantParseThis";

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders
                        .get(
                                "/stats/{userId}/devices/lastSeen?start={start}&end={end}",
                                userId,
                                end,
                                start
                        )
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("type").isNotEmpty())
                .andExpect(jsonPath("detail").isNotEmpty())
                .andExpect(jsonPath("errors").isArray())
                .andExpect(jsonPath("errors").isNotEmpty());
    }

    @Test
    public void getSeen_shouldReturnProperlyFormattedJson() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final DataSet dataSet = prepareRandomDataSet();
        when(statsManager.getSeenDeviceCount(userId, OffsetDateTime.MIN, OffsetDateTime.MAX)).thenReturn(dataSet);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/stats/{userId}/devices/seen", userId)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(statsManager).getSeenDeviceCount(userId, OffsetDateTime.MIN, OffsetDateTime.MAX);
        result.andExpect(DataSetMatcher.from(dataSet));
    }

    @Test
    public void getSeen_withBoundaries_shouldUseCorrectDates() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final DataSet dataSet = prepareRandomDataSet();
        final String start = "2000-01-01T21:49:53.666Z";
        final String end = "2012-12-21T21:49:53.666Z";
        final OffsetDateTime startDate = OffsetDateTime.parse(start);
        final OffsetDateTime endDate = OffsetDateTime.parse(end);
        when(statsManager.getSeenDeviceCount(userId, startDate, endDate)).thenReturn(dataSet);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders
                        .get(
                                "/stats/{userId}/devices/seen?start={start}&end={end}",
                                userId,
                                start,
                                end
                        )
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(statsManager).getSeenDeviceCount(userId, startDate, endDate);
        result.andExpect(DataSetMatcher.from(dataSet));
    }

    @Test
    public void getSeen_withIncorrectDate_shouldReturnBadRequest() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final Date start = new Date(123456789999L);
        final Date end = new Date(123456789000L);
        final DateFormatter dateFormatter = new DateFormatter();
        dateFormatter.setIso(DateTimeFormat.ISO.DATE_TIME);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders
                        .get(
                                "/stats/{userId}/devices/seen?start={start}&end={end}",
                                userId,
                                dateFormatter.print(start, Locale.US),
                                dateFormatter.print(end, Locale.US)
                        )
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void getDeviceCountPerUserId_shouldReturnProperlyFormattedJson() throws Exception {
        // Given
        final DataSet dataSet = prepareRandomDataSet();
        when(statsManager.getDeviceCountPerUserId()).thenReturn(dataSet);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.get("/stats/devices/perUserId")
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(statsManager).getDeviceCountPerUserId();
        result.andExpect(DataSetMatcher.from(dataSet));
    }

    private DataSet prepareRandomDataSet() {
        final SecureRandom random = new SecureRandom();
        final DataSet.Builder builder = DataSet.builder();
        long total = 0;
        for (int versionIdx = 0; versionIdx < 10; versionIdx++) {
            String versionId = UUID.randomUUID().toString();
            long count = random.nextLong() & Long.MAX_VALUE;
            builder.value(versionId, BigDecimal.valueOf(count));
            total += count;
        }
        return builder.total(BigDecimal.valueOf(total)).build();
    }

    private static class DataSetMatcher implements ResultMatcher {
        final ArrayList<ResultMatcher> matchers;

        private DataSetMatcher(DataSet expected) {
            this.matchers = new ArrayList<>(expected.getValues().size() + 1);
            matchers.add(jsonPath("total").value(anyOf(equalTo(expected.getTotal().intValue()), equalTo(expected.getTotal().longValue()))));
            for (Map.Entry<String, BigDecimal> stat : expected.getValues().entrySet()) {
                matchers.add(jsonPath("values.%s", stat.getKey()).value(anyOf(equalTo(stat.getValue().longValue()), equalTo(stat.getValue().intValue()))));
            }
        }

        static ResultMatcher from(DataSet expected) {
            return new DataSetMatcher(expected);
        }

        @Override
        public void match(MvcResult result) throws Exception {
            for (ResultMatcher matcher : matchers) {
                matcher.match(result);
            }
        }
    }

}
