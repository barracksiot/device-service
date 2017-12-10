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

package io.barracks.deviceservice.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.deviceservice.manager.DeviceManager;
import io.barracks.deviceservice.model.DeviceEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DeviceMessageReceiver {

    private final DeviceManager deviceManager;
    private final ObjectMapper objectMapper;
    private final CounterService counter;

    @Autowired
    DeviceMessageReceiver(DeviceManager deviceManager, ObjectMapper objectMapper, CounterService counter) {
        this.deviceManager = deviceManager;
        this.objectMapper = objectMapper;
        this.counter = counter;
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "${io.barracks.deviceservice.queuename}", durable = "true", autoDelete = "false"),
                    exchange = @Exchange(value = "${io.barracks.amqp.exchangename}", type = "fanout", durable = "true"),
                    key = "${io.barracks.deviceservice.routingkey}"
            )
    )
    public void receiveMessage(@Payload DeviceEvent deviceEvent) {
        try {
            deviceManager.saveDeviceEvent(deviceEvent);
            incrementRabbitMQMetric("success");
        } catch (Exception e) {
            log.error("Error while saving device event into database.", e);
            incrementRabbitMQMetric("error");
        }
    }

    private void incrementRabbitMQMetric(String status) {
        counter.increment("message.process.device.event." + status);
    }


}