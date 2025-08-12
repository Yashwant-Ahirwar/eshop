package com.mindfultech.acadmy.eshop.service;

import com.mindfultech.acadmy.eshop.model.Order;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransformingConsumer {

    private final KafkaTemplate<String, Order> kafkaTemplate;

    public TransformingConsumer(KafkaTemplate<String,Order> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "input-topic", containerFactory = "kafkaListenerContainerFactory")
    public void handle(ConsumerRecord<String,Order> record) {
        Order value = record.value();
        // apply business logic
        kafkaTemplate.send("output-topic", record.key(), value);
        // If this method exits normally, the container will commit offsets only when it commits the transaction.
        // If an exception is thrown, the transaction will be aborted and offsets will not be committed.
    }
}
