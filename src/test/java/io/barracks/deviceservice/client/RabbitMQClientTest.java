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

package io.barracks.deviceservice.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.barracks.deviceservice.client.entity.DeviceChangeEntity;
import io.barracks.deviceservice.model.DeviceEvent;
import io.barracks.deviceservice.model.DeviceRequest;
import io.barracks.deviceservice.model.EventType;
import io.barracks.deviceservice.utils.DeviceEventUtils;
import io.barracks.deviceservice.utils.DeviceRequestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.actuate.metrics.CounterService;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class RabbitMQClientTest {

    private RabbitMQClient rabbitMQClient;

    private String enrollmentExchangeName = "test_enrollment_exchange";
    private String enrollmentRoutingKey = "test_enrollment_routing_key";
    private String deviceDataExchangeName = "test_devicedata_exchange";
    private String deviceDataRoutingKey = "test_devicedata_routing_key";
    private String devicePackageExchangeName = "test_devicepackage_exchange";
    private String devicePackageRoutingKey = "test_devicepackage_routing_key";

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private CounterService counterService;

    @Before
    public void setUp() {
        rabbitMQClient = new RabbitMQClient(
                enrollmentExchangeName,
                enrollmentRoutingKey,
                deviceDataExchangeName,
                deviceDataRoutingKey,
                devicePackageExchangeName,
                devicePackageRoutingKey,
                rabbitTemplate,
                counterService);
    }

    @Test
    public void postDeviceEnrollment_whenServiceSucceeds_serverShouldBeCalled() throws JsonProcessingException {
        // Given
        final DeviceEvent deviceEvent = DeviceEventUtils.getDeviceEvent();

        // When
        rabbitMQClient.postEnrollmentEvent(deviceEvent);

        // Then
        verify(rabbitTemplate).convertAndSend(enrollmentExchangeName, enrollmentRoutingKey, deviceEvent);
    }

    @Test
    public void postDeviceEnrollment_whenServiceFails_shouldLogError() throws JsonProcessingException {
        // Given
        final DeviceEvent deviceEvent = DeviceEventUtils.getDeviceEvent();
        doThrow(Exception.class).when(rabbitTemplate).convertAndSend(enrollmentExchangeName, enrollmentRoutingKey, deviceEvent);

        // When
        rabbitMQClient.postEnrollmentEvent(deviceEvent);

        // Then
        verify(rabbitTemplate).convertAndSend(enrollmentExchangeName, enrollmentRoutingKey, deviceEvent);
    }

    @Test
    public void postCustomClientDataChange_whenServiceSucceeds_serverShouldBeCalled() throws JsonProcessingException {
        // Given
        final DeviceRequest deviceRequest = DeviceRequestUtils.getDeviceRequest();
        final DeviceEvent deviceEvent = DeviceEventUtils.getDeviceEvent();
        final DeviceChangeEntity deviceChangeEntity = DeviceChangeEntity.builder()
                .oldRequest(deviceRequest)
                .deviceEvent(deviceEvent)
                .build();

        // When
        rabbitMQClient.postDeviceChange(deviceRequest, deviceEvent, EventType.DEVICE_DATA_CHANGE);

        // Then
        verify(rabbitTemplate).convertAndSend(deviceDataExchangeName, deviceDataRoutingKey, deviceChangeEntity);
    }

    @Test
    public void postCustomClientDataChange_whenServiceFails_shouldLogError() throws JsonProcessingException {
        // Given
        final DeviceRequest deviceRequest = DeviceRequestUtils.getDeviceRequest();
        final DeviceEvent deviceEvent = DeviceEventUtils.getDeviceEvent();
        final DeviceChangeEntity deviceChangeEntity = DeviceChangeEntity.builder()
                .oldRequest(deviceRequest)
                .deviceEvent(deviceEvent)
                .build();
        doThrow(Exception.class).when(rabbitTemplate).convertAndSend(deviceDataExchangeName, deviceDataRoutingKey, deviceEvent);

        // When
        rabbitMQClient.postDeviceChange(deviceRequest, deviceEvent, EventType.DEVICE_DATA_CHANGE);

        // Then
        verify(rabbitTemplate).convertAndSend(deviceDataExchangeName, deviceDataRoutingKey, deviceChangeEntity);
    }

    @Test
    public void postDevicePackageChange_whenServiceSucceeds_serverShouldBeCalled() throws JsonProcessingException {
        // Given
        final DeviceRequest deviceRequest = DeviceRequestUtils.getDeviceRequest();
        final DeviceEvent deviceEvent = DeviceEventUtils.getDeviceEvent();
        final DeviceChangeEntity deviceChangeEntity = DeviceChangeEntity.builder()
                .oldRequest(deviceRequest)
                .deviceEvent(deviceEvent)
                .build();

        // When
        rabbitMQClient.postDeviceChange(deviceRequest, deviceEvent, EventType.DEVICE_PACKAGE_CHANGE);

        // Then
        verify(rabbitTemplate).convertAndSend(devicePackageExchangeName, devicePackageRoutingKey, deviceChangeEntity);
    }

    @Test
    public void postDevicePackageChange_whenServiceFails_shouldLogError() throws JsonProcessingException {
        // Given
        final DeviceRequest deviceRequest = DeviceRequestUtils.getDeviceRequest();
        final DeviceEvent deviceEvent = DeviceEventUtils.getDeviceEvent();
        final DeviceChangeEntity deviceChangeEntity = DeviceChangeEntity.builder()
                .oldRequest(deviceRequest)
                .deviceEvent(deviceEvent)
                .build();
        doThrow(Exception.class).when(rabbitTemplate).convertAndSend(devicePackageExchangeName, devicePackageRoutingKey, deviceEvent);

        // When
        rabbitMQClient.postDeviceChange(deviceRequest, deviceEvent, EventType.DEVICE_PACKAGE_CHANGE);

        // Then
        verify(rabbitTemplate).convertAndSend(devicePackageExchangeName, devicePackageRoutingKey, deviceChangeEntity);
    }

}
