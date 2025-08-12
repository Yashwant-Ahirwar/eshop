package com.mindfultech.acadmy.eshop.producer;

import com.mindfultech.acadmy.eshop.config.KafkaConfig;
import com.mindfultech.acadmy.eshop.model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderProducer {

    private final KafkaTemplate<String, Order> kafkaTemplate;

    @Autowired
    public OrderProducer(KafkaTemplate<String, Order> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

//    public void sendOrder(Order order) {
//        kafkaTemplate.send("orders", order.getOrderId(), order);
//    }

    public void sendOrder(Order order) {
        kafkaTemplate.executeInTransaction(operations -> {
            operations.send(KafkaConfig.TOPIC_NAME, order.getOrderId(), order);
            return true;
        });
    }
}
