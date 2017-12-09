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

import com.google.common.collect.ImmutableMap;
import io.barracks.deviceservice.Application;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.URI;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration({Application.class, EmbeddedMongoAutoConfiguration.class})
@WebIntegrationTest(randomPort = true)
public class DeviceResourceLoadTest extends DeviceResourceBaseTest {
    private final Logger logger = LoggerFactory.getLogger(DeviceResourceLoadTest.class);

    @Test
    public void loadWithManyPingsAndRequestForPaginatedData_shouldWorkEvenWithLotsOfData() throws Exception {
        final int userCount = 1;
        final int deviceCount = 100;
        final int segmentsCount = 3;
        final int pingCount = 120;
        final String requestUrl = getBaseUrl() + "/devices";
        final String perSegmentRequestUrl = getBaseUrl() + "/segments/{segmentId}/devices?what=ever";
        final String perOtherSegmentUrl = getBaseUrl() + "/segments/other/devices?userId={userId}";
        final String perUnitRequestUrl = getBaseUrl() + "/devices/{unitId}/events?userId={userId}";
        final String perUnitWithUnchangedRequestUrl = getBaseUrl() + "/devices/{unitId}/events?userId={userId}&onlyChanged=false";
        final HttpHeaders jsonHeaders = getJsonRequestHeaders();
        final AtomicLong counter = new AtomicLong();
        final Map<String, List<String>> userDevices = new HashMap<>(userCount);
        final Map<String, List<String>> userSegments = new HashMap<>(userCount);
        final ExecutorService service = Executors.newFixedThreadPool(32);

        ArrayList<VoidCallable> callables = new ArrayList<>(userCount * deviceCount * segmentsCount);
        for (int userIdx = 0; userIdx < userCount; userIdx++) {
            final String userId = UUID.randomUUID().toString();
            List<String> devices = new ArrayList<>(deviceCount);
            userDevices.put(userId, devices);
            List<String> segments = new ArrayList<>(segmentsCount);
            userSegments.put(userId, segments);
            for (int segmentIdx = 0; segmentIdx < segmentsCount; segmentIdx++) {
                for (int deviceIdx = 0; deviceIdx < deviceCount; deviceIdx++) {
                    final JSONObject deviceEventRequest = getDeviceEventRequest();
                    final String unitId = UUID.randomUUID().toString();
                    devices.add(unitId);
                    deviceEventRequest.replace("userId", userId);
                    deviceEventRequest.replace("unitId", unitId);
                    callables.add(new DeviceSimulator(requestUrl, deviceEventRequest, jsonHeaders, pingCount, counter));
                }
                if (segmentIdx < segmentsCount - 1) {
                    final JSONObject segmentRequest = getSegmentRequest(userId, devices.subList(deviceCount * segmentIdx, deviceCount * (segmentIdx + 1)));
                    final String segmentId = testRestTemplate.exchange(
                            getBaseUrl() + CREATE_SEGMENT_URL,
                            HttpMethod.POST,
                            new HttpEntity<>(segmentRequest.toJSONString(), getJsonRequestHeaders()),
                            JSONObject.class,
                            segmentRequest
                    ).getBody().getAsString("id");
                    segments.add(segmentId);
                }
            }
            final JSONArray order = new JSONArray();
            order.addAll(segments);
            final HttpStatus status = testRestTemplate.exchange(
                    getBaseUrl() + UPDATE_SEGMENT_ORDER_URL,
                    HttpMethod.PUT,
                    new HttpEntity<Object>(order.toJSONString(), getJsonRequestHeaders()),
                    Void.class,
                    userId
            ).getStatusCode();
            assertThat(status.is2xxSuccessful()).isTrue();
            segments.add(null);
        }
        callables.add(0, new CountWatcher(counter, userCount * segmentsCount * deviceCount * pingCount));
        List<Future<Void>> futures = service.invokeAll(callables);
        for (Future<Void> future : futures) {
            future.get();
        }

        logger.warn("Running tests on the paginated queries");
        callables = new ArrayList<>(userCount * segmentsCount + userCount * deviceCount + 1);
        for (Map.Entry<String, List<String>> userSegment : userSegments.entrySet()) {
            String userId = userSegment.getKey();
            for (String segmentId : userSegment.getValue()) {
                if (segmentId != null) {
                    URI uri = testRestTemplate.getUriTemplateHandler().expand(perSegmentRequestUrl, segmentId);
                    callables.add(new MemberSimulator(uri.toString(), getJsonRequestHeaders(), 20, deviceCount / 20));
                } else {
                    URI uri = testRestTemplate.getUriTemplateHandler().expand(perOtherSegmentUrl, userId);
                    callables.add(new MemberSimulator(uri.toString(), getJsonRequestHeaders(), 20, deviceCount / 20));
                }
            }
            for (String unitId : userDevices.get(userId)) {
                URI uri = testRestTemplate.getUriTemplateHandler().expand(perUnitRequestUrl, unitId, userId);
                callables.add(new MemberSimulator(uri.toString(), getJsonRequestHeaders(), 20, pingCount / 20 / 2));
            }
            for (String unitId : userDevices.get(userId)) {
                URI uri = testRestTemplate.getUriTemplateHandler().expand(perUnitWithUnchangedRequestUrl, unitId, userId);
                callables.add(new MemberSimulator(uri.toString(), getJsonRequestHeaders(), 20, pingCount / 20));
            }
        }
        futures = service.invokeAll(callables);
        for (Future<Void> future : futures) {
            future.get();
        }
    }

    static abstract class VoidCallable implements Callable<Void> {

    }

    static final class CountWatcher extends VoidCallable {
        private final Logger logger = LoggerFactory.getLogger(CountWatcher.class);
        private final AtomicLong counter;
        private final long maxValue;

        CountWatcher(AtomicLong counter, long maxValue) {
            this.counter = counter;
            this.maxValue = maxValue;
        }

        @Override
        public Void call() {
            while (counter.get() < maxValue) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                logger.warn("Ran {} queries on {}", counter.get(), maxValue);
            }
            return null;
        }
    }

    static final class DeviceSimulator extends VoidCallable {
        private final TestRestTemplate testRestTemplate = new TestRestTemplate();
        private final String requestUrl;
        private final int requestCount;
        private final AtomicLong counter;
        private final JSONObject deviceRequest;
        private final HttpHeaders headers;

        DeviceSimulator(String requestUrl, JSONObject jsonRequest, HttpHeaders headers, int requestCount, AtomicLong counter) {
            this.requestUrl = requestUrl;
            this.requestCount = requestCount;
            this.counter = counter;
            this.headers = headers;
            this.deviceRequest = jsonRequest;
        }


        @Override
        public Void call() throws Exception {
            for (int i = 0; i < requestCount; i++) {
                if (i % 2 == 0) {
                    deviceRequest.replace("additionalProperties", ImmutableMap.of("key", UUID.randomUUID().toString()));
                }
                counter.incrementAndGet();
                HttpEntity request = new HttpEntity<>(deviceRequest.toJSONString(), headers);
                ResponseEntity<?> response = testRestTemplate.exchange(
                        requestUrl,
                        HttpMethod.POST,
                        request,
                        Void.class
                );
                assertTrue(response.getStatusCode().is2xxSuccessful());
            }
            return null;
        }
    }

    static final class MemberSimulator extends VoidCallable {
        private final Logger logger = LoggerFactory.getLogger(MemberSimulator.class);
        private final TestRestTemplate testRestTemplate = new TestRestTemplate();
        private final String baseUrl;
        private final HttpEntity requestEntity;
        private final long pageSize;
        private final long totalPages;

        MemberSimulator(String baseUrl, HttpHeaders headers, long pageSize, long totalPages) {
            this.baseUrl = baseUrl;
            this.requestEntity = new HttpEntity(headers);
            this.pageSize = pageSize;
            this.totalPages = totalPages;
        }

        @Override
        public Void call() throws Exception {
            final String pageUrl = baseUrl + "&page={pageNumber}&size={pageSize}";
            for (int i = 0; i < totalPages; i++) {
                ResponseEntity<Map<String, Object>> response = testRestTemplate.exchange(
                        pageUrl,
                        HttpMethod.GET,
                        requestEntity,
                        new ParameterizedTypeReference<Map<String, Object>>() {
                        },
                        i,
                        pageSize
                );
                assertTrue(
                        MessageFormatter.format("Request for " + pageUrl + " failed with code {} and response:\n{}", response.getStatusCode().value(), response.getBody()).getMessage(),
                        response.getStatusCode().is2xxSuccessful()
                );
                @SuppressWarnings("unchecked")
                Map<String, Object> page = (Map<String, Object>) response.getBody().get("page");
                if (i == 0) {
                    logger.warn("Loaded page 0 for {} :\n\t{} pages (expected {})\n\t{} elements", baseUrl, page.get("totalPages"), this.totalPages, page.get("totalElements"));
                }
                assertThat(page.get("totalPages")).isIn(this.totalPages, (int) this.totalPages);
                assertThat(page.get("totalElements")).isIn(this.pageSize * totalPages, (int) (this.pageSize * totalPages));
            }
            return null;
        }
    }
}
