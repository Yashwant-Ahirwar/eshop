package com.mindfultech.acadmy.eshop.consumer;

import com.mindfultech.acadmy.eshop.model.Order;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReliableOrderConsumer {

    @KafkaListener(topics = "orders", containerFactory = "kafkaListenerContainerFactory")
    public void listen(ConsumerRecord<String, Order> record) {
        Order order = record.value();
        System.out.println("Processing Order in TX: " + order);

        // Simulate failure for testing DLQ
        if (order.getQuantity() < 0) {
            throw new IllegalArgumentException("Invalid order quantity");
        }

        // Otherwise, process successfully
        System.out.println("Order processed successfully: " + order);
    }
}
