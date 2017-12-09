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

import io.barracks.commons.test.WebApplicationTest;
import io.barracks.deviceservice.Application;
import io.barracks.deviceservice.config.ExceptionConfig;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration({Application.class, EmbeddedMongoAutoConfiguration.class, ExceptionConfig.class})
@WebIntegrationTest(randomPort = true)
public class ConfigurationTest extends WebApplicationTest {

    private RestTemplate testRestTemplate;

    @Before
    public void setUp() {
        testRestTemplate = new RestTemplate();
    }

    @Test
    public void getUnknownEndpoint_shouldReturnProperlyFormatted404() {
        // When
        JSONObject jsonObject = null;
        try {
            testRestTemplate.getForEntity(
                    getBaseUrl() + "/whatever",
                    String.class
            );
        } catch (HttpStatusCodeException e) {
            jsonObject = (JSONObject) JSONValue.parse(e.getResponseBodyAsString());
        }
        // Then
        assertThat(jsonObject).isNotNull();
        assertThat(jsonObject.getAsNumber("status").intValue()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }
}