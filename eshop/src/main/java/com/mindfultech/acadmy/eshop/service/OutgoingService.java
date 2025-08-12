package com.mindfultech.acadmy.eshop.service;

import com.mindfultech.acadmy.eshop.model.Order;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutgoingService {

    private final KafkaTemplate<String, Order> kafkaTemplate;

    public OutgoingService(KafkaTemplate<String,Order> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional("kafkaTransactionManager")
    public void sendTransactional(String topic, Order msg) {
        kafkaTemplate.send(topic, msg);
        // an exception will roll back the transaction
    }
}
