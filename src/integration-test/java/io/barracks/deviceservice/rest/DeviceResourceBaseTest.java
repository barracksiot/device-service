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

import com.mongodb.BasicDBObject;
import com.mongodb.Mongo;
import io.barracks.commons.test.WebApplicationTest;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.ParseException;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public abstract class DeviceResourceBaseTest extends WebApplicationTest {
    static final String GET_SET_CONFIGURATION_URL = "/devices/{unitId}/configuration?userId={userId}";
    static final String CREATE_SEGMENT_URL = "/segments";
    static final String UPDATE_SEGMENT_ORDER_URL = "/segments/order?userId={userId}";
    MockRestServiceServer mockServer;
    TestRestTemplate testRestTemplate;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private Mongo mongo;
    @Autowired
    private RestTemplate restTemplate;

    @Before
    public void setUp() throws Exception {
        mongoTemplate.getCollection("units").remove(new BasicDBObject());
        mongoTemplate.getCollection("devices").remove(new BasicDBObject());
        mongoTemplate.getCollection("unitConfigurations").remove(new BasicDBObject());
        mockServer = MockRestServiceServer.createServer(restTemplate);
        testRestTemplate = new TestRestTemplate();
    }


    HttpHeaders getJsonRequestHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }

    JSONObject getDeviceEventRequest() throws Exception {
        final JSONObject deviceEventRequest = getJsonFromResource("addDeviceEventRequest");
        deviceEventRequest.replace("userId", UUID.randomUUID().toString());
        deviceEventRequest.replace("unitId", UUID.randomUUID().toString());
        deviceEventRequest.replace("versionId", UUID.randomUUID().toString());
        return deviceEventRequest;
    }

    JSONObject getConfigurationRequest() throws Exception {
        final JSONObject configurationRequest = getJsonFromResource("setConfigurationRequest");
        return configurationRequest;
    }

    JSONObject getSegmentRequest(String userId, String unitId) throws Exception {
        return getSegmentRequest(userId, Collections.singletonList(unitId));
    }

    JSONObject getSegmentRequest(String userId, Collection<String> unitIds) throws Exception {
        final JSONObject segmentRequest = getJsonFromResource("createSegmentRequest");
        segmentRequest.replace("name", UUID.randomUUID().toString().replace("-", ""));
        segmentRequest.replace("userId", userId);
        ((JSONArray) ((JSONObject) ((JSONObject) segmentRequest.get("query")).get("in")).get("unitId")).addAll(unitIds);
        return segmentRequest;
    }

    @Override
    public JSONObject getJsonFromResource(String name) throws IOException, ParseException {
        return super.getJsonFromResource(DeviceResourceBaseTest.class, name);
    }
}
