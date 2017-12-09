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

import io.barracks.deviceservice.Application;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration({Application.class, EmbeddedMongoAutoConfiguration.class})
@WebIntegrationTest(randomPort = true)
public class DeviceResourceTest extends DeviceResourceBaseTest {
    @Test
    public void addDeviceEvent_whitNoMatchingSegment_shouldAddDeviceWithEmptySegmentId() throws Exception {
        // Given
        final JSONObject deviceEventRequest = getDeviceEventRequest();
        final JSONObject expected = getJsonFromResource("addDeviceEventResponse");
        expected.replace("userId", deviceEventRequest.getAsString("userId"));
        expected.replace("unitId", deviceEventRequest.getAsString("unitId"));
        expected.replace("versionId", deviceEventRequest.getAsString("versionId"));

        // When
        ResponseEntity<JSONObject> response = testRestTemplate.exchange(
                getBaseUrl() + "/devices",
                HttpMethod.POST,
                new HttpEntity<Object>(deviceEventRequest.toJSONString(), getJsonRequestHeaders()),
                JSONObject.class
        );

        // Then
        assertThat(response.getBody()).containsAllEntriesOf(expected);
        assertThat(response.getBody().getAsString("segmentId")).isNull();
    }

    @Test
    public void addDeviceEvent_withMatchingSegmentNotActive_shouldAddDeviceWithNoSegmentId() throws Exception {
        // Given
        final JSONObject deviceEventRequest = getDeviceEventRequest();
        final JSONObject segmentRequest = getSegmentRequest(deviceEventRequest.getAsString("userId"), deviceEventRequest.getAsString("unitId"));
        final JSONObject expected = getJsonFromResource("addDeviceEventResponse");
        expected.replace("userId", deviceEventRequest.getAsString("userId"));
        expected.replace("unitId", deviceEventRequest.getAsString("unitId"));
        expected.replace("versionId", deviceEventRequest.getAsString("versionId"));
        testRestTemplate.exchange(
                getBaseUrl() + CREATE_SEGMENT_URL,
                HttpMethod.POST,
                new HttpEntity<Object>(segmentRequest.toJSONString(), getJsonRequestHeaders()),
                JSONObject.class
        );

        // When
        ResponseEntity<JSONObject> response = testRestTemplate.exchange(
                getBaseUrl() + "/devices",
                HttpMethod.POST,
                new HttpEntity<Object>(deviceEventRequest.toJSONString(), getJsonRequestHeaders()),
                JSONObject.class
        );

        // Then
        assertThat(response.getBody()).containsAllEntriesOf(expected);
        assertThat(response.getBody().getAsString("segmentId")).isNull();
    }

    @Test
    public void addDeviceEvent_withMatchingSegmentActive_shouldAddDeviceWithNoSegmentId() throws Exception {
        // Given
        final JSONObject deviceEventRequest = getDeviceEventRequest();
        final JSONObject segmentRequest = getSegmentRequest(deviceEventRequest.getAsString("userId"), deviceEventRequest.getAsString("unitId"));
        final JSONObject expected = getJsonFromResource("addDeviceEventResponse");
        expected.replace("userId", deviceEventRequest.getAsString("userId"));
        expected.replace("unitId", deviceEventRequest.getAsString("unitId"));
        expected.replace("versionId", deviceEventRequest.getAsString("versionId"));
        final JSONObject activeSegment = testRestTemplate.exchange(
                getBaseUrl() + CREATE_SEGMENT_URL,
                HttpMethod.POST,
                new HttpEntity<Object>(segmentRequest.toJSONString(), getJsonRequestHeaders()),
                JSONObject.class
        ).getBody();
        final JSONArray order = new JSONArray();
        order.add(activeSegment.getAsString("id"));
        testRestTemplate.exchange(
                getBaseUrl() + UPDATE_SEGMENT_ORDER_URL,
                HttpMethod.PUT,
                new HttpEntity<Object>(order.toJSONString(), getJsonRequestHeaders()),
                JSONArray.class,
                deviceEventRequest.getAsString("userId")
        );

        // When
        ResponseEntity<JSONObject> response = testRestTemplate.exchange(
                getBaseUrl() + "/devices",
                HttpMethod.POST,
                new HttpEntity<Object>(deviceEventRequest.toJSONString(), getJsonRequestHeaders()),
                JSONObject.class
        );

        // Then
        assertThat(response.getBody()).containsAllEntriesOf(expected);
        assertThat(response.getBody().getAsString("segmentId")).isNotEmpty();
    }

    @Test
    public void getConfiguration_whenNotSet_shouldReturnDefaultConfiguration() throws Exception {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final JSONObject expected = getJsonFromResource("setConfigurationResponse");
        expected.replace("userId", userId);
        expected.replace("unitId", unitId);

        // When
        ResponseEntity<JSONObject> response = testRestTemplate.exchange(
                getBaseUrl() + GET_SET_CONFIGURATION_URL,
                HttpMethod.GET,
                new HttpEntity<>(getJsonRequestHeaders()),
                JSONObject.class,
                unitId,
                userId
        );

        // Then
        assertThat(response.getBody()).containsAllEntriesOf(expected);
    }

    @Test
    public void getConfiguration_whenAlreadySet_shouldReturnPreviousConfiguration() throws Exception {
        // Given
        final JSONObject configurationRequest = getConfigurationRequest();
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final JSONObject expected = getJsonFromResource("setConfigurationResponse");
        expected.replace("userId", userId);
        expected.replace("unitId", unitId);

        // When
        testRestTemplate.exchange(
                getBaseUrl() + GET_SET_CONFIGURATION_URL,
                HttpMethod.PUT,
                new HttpEntity<Object>(configurationRequest.toJSONString(), getJsonRequestHeaders()),
                JSONObject.class,
                userId, unitId
        );
        ResponseEntity<JSONObject> response = testRestTemplate.exchange(
                getBaseUrl() + GET_SET_CONFIGURATION_URL,
                HttpMethod.GET,
                new HttpEntity<>(getJsonRequestHeaders()),
                JSONObject.class,
                unitId,
                userId
        );

        // Then
        assertThat(response.getBody()).containsAllEntriesOf(expected);
    }

    @Test
    public void setConfiguration_whenAllIsFine_shouldReturnConfiguration() throws Exception {
        // Given
        final JSONObject configurationRequest = getConfigurationRequest();
        final String userId = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final JSONObject expected = getJsonFromResource("setConfigurationResponse");
        expected.replace("userId", userId);
        expected.replace("unitId", unitId);

        // When
        ResponseEntity<JSONObject> response = testRestTemplate.exchange(
                getBaseUrl() + GET_SET_CONFIGURATION_URL,
                HttpMethod.POST,
                new HttpEntity<Object>(configurationRequest.toJSONString(), getJsonRequestHeaders()),
                JSONObject.class,
                unitId,
                userId
        );

        // Then
        assertThat(response.getBody()).containsAllEntriesOf(expected);
    }

}
