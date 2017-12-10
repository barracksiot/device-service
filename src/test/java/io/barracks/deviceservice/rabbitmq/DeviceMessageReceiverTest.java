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

import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMq;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMqConfig;
import io.barracks.deviceservice.Application;
import io.barracks.deviceservice.config.RabbitMQConfig;
import io.barracks.deviceservice.manager.DeviceManager;
import io.barracks.deviceservice.model.DeviceEvent;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.junit.BrokerRunning;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.FileCopyUtils;

import java.util.concurrent.ExecutionException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {RabbitMQConfig.class, Application.class})
@TestPropertySource(locations="classpath:application-test.properties")
public class DeviceMessageReceiverTest {

    private static EmbeddedRabbitMq rabbitMq;

    @SpyBean
    private DeviceMessageReceiver receiver;

    @Value("${io.barracks.amqp.exchangename}")
    private String exchangeName;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private DeviceManager deviceManager;

    @BeforeClass
    public static void startBroker() throws ExecutionException, InterruptedException {
        EmbeddedRabbitMqConfig config = new EmbeddedRabbitMqConfig.Builder()
                .erlangCheckTimeoutInMillis(150000)
                .rabbitMqServerInitializationTimeoutInMillis(100000)
                .defaultRabbitMqCtlTimeoutInMillis(100000)
                .downloadConnectionTimeoutInMillis(150000)
                .downloadReadTimeoutInMillis(150000)
                .build();
        rabbitMq = new EmbeddedRabbitMq(config);
        rabbitMq.start();

    }

    @AfterClass
    public static void clear() throws Exception {
        rabbitMq.stop();
    }

    @Before
    public void isBrokerRunning() {
        BrokerRunning.isRunning();
    }

    @Test
    public void receiveMessage_whenAllIsFine_shouldCallManager() throws Exception {
        final ClassPathResource resource = new ClassPathResource("/io/barracks/deviceservice/event.json", DeviceEvent.class);
        final MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType(MediaType.APPLICATION_JSON_VALUE);
        this.rabbitTemplate.convertSendAndReceive(exchangeName, "test.v2.afsdsf", new Message(FileCopyUtils.copyToByteArray(resource.getInputStream()), messageProperties));

        verify(receiver).receiveMessage(any());
        verify(deviceManager).saveDeviceEvent(any());
    }

}