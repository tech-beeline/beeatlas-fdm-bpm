/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.AuthSSOClient;

@Slf4j
@Component
public class RabbitService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private CachingConnectionFactory connectionFactory;

    @Autowired
    private AuthSSOClient authSSOClient;

    public void sendMessage(String queue, Object message) {
        try {
            if (!isConnected()) {
                refreshConnection();
            }

            rabbitTemplate.convertAndSend(queue, message, messagePostProcessor -> {
                messagePostProcessor.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                return messagePostProcessor;
            });
        } catch (Exception e) {
            log.error("Error sending message: ", e);
        }
    }

    public void sendToFanoutTcExchange(String exchangeName, Object message) {
        try {
            if (!isConnected()) {
                refreshConnection();
            }
            rabbitTemplate.convertAndSend(exchangeName, "", message, messagePostProcessor -> {
                messagePostProcessor.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                return messagePostProcessor;
            });
            log.info("Message sent to fanout exchange: {}", exchangeName);
        } catch (Exception e) {
            log.error("Error sending message to fanout exchange: ", e);
        }
    }

    private boolean isConnected() {
        try {
            connectionFactory.createConnection().close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void refreshConnection() {
        try {
            connectionFactory.resetConnection();
            connectionFactory.setPassword(authSSOClient.getToken());
        } catch (Exception e) {
            log.error("Error refreshing connection: ", e);
        }
    }
}