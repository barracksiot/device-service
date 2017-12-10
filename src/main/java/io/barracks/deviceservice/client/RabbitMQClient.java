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

import io.barracks.deviceservice.client.entity.DeviceChangeEntity;
import io.barracks.deviceservice.model.DeviceEvent;
import io.barracks.deviceservice.model.DeviceRequest;
import io.barracks.deviceservice.model.EventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RabbitMQClient {

    private final RabbitTemplate rabbitTemplate;
    private final CounterService counter;
    private String enrollmentExchangeName;
    private String enrollmentRoutingKey;
    private String customClientDataExchangeName;
    private String customClientDataRoutingKey;
    private String devicePackageExchangeName;
    private String devicePackageRoutingKey;

    @Autowired
    public RabbitMQClient(
            @Value("${io.barracks.enrollment.exchangename}") String enrollmentExchangeName,
            @Value("${io.barracks.enrollment.routingkey}") String enrollmentRoutingKey,
            @Value("${io.barracks.customclientdata.exchangename}") String customClientDataExchangeName,
            @Value("${io.barracks.customclientdata.routingkey}") String customClientDataRoutingKey,
            @Value("${io.barracks.devicepackage.exchangename}") String devicePackageExchangeName,
            @Value("${io.barracks.devicepackage.routingkey}") String devicePackageRoutingKey,
            RabbitTemplate rabbitTemplate,
            CounterService counter
    ) {
        this.enrollmentExchangeName = enrollmentExchangeName;
        this.enrollmentRoutingKey = enrollmentRoutingKey;
        this.customClientDataExchangeName = customClientDataExchangeName;
        this.customClientDataRoutingKey = customClientDataRoutingKey;
        this.devicePackageExchangeName = devicePackageExchangeName;
        this.devicePackageRoutingKey = devicePackageRoutingKey;
        this.rabbitTemplate = rabbitTemplate;
        this.counter = counter;
    }

    public void postEnrollmentEvent(DeviceEvent deviceEvent) {
        try {
            rabbitTemplate.convertAndSend(enrollmentExchangeName, enrollmentRoutingKey, deviceEvent);
            incrementRabbitMQMetric("success");
        } catch (Exception e) {
            log.error("The message cannot be sent to RabbitMQ. It is possible that the broker is not running. Exception : " + e);
            incrementRabbitMQMetric("error");
        }
    }

    public void postDeviceChange(DeviceRequest oldRequest, DeviceEvent deviceEvent, EventType eventType) {
        try {
            final DeviceChangeEntity deviceChangeEntity = DeviceChangeEntity.builder()
                    .oldRequest(oldRequest)
                    .deviceEvent(deviceEvent)
                    .build();

            if (eventType.equals(EventType.DEVICE_DATA_CHANGE)) {
                rabbitTemplate.convertAndSend(customClientDataExchangeName, customClientDataRoutingKey, deviceChangeEntity);
            } else if (eventType.equals(EventType.DEVICE_PACKAGE_CHANGE)) {
                rabbitTemplate.convertAndSend(devicePackageExchangeName, devicePackageRoutingKey, deviceChangeEntity);
            }
            incrementRabbitMQMetric("success");
        } catch (Exception e) {
            log.error("The message cannot be sent to RabbitMQ. It is possible that the broker is not running. Exception : " + e);
            incrementRabbitMQMetric("error");
        }
    }

    private void incrementRabbitMQMetric(String status) {
        counter.increment("message.sent.device." + status);
    }

}
