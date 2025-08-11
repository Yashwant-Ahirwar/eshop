package com.mindfultech.acadmy.eshop.consumer;

import com.mindfultech.acadmy.eshop.model.Order;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderConsumer {

    @KafkaListener(topics = "orders", groupId = "order-group")
    public void listen(Order order) {
        System.out.println("Received Order: " + order);
    }
}
