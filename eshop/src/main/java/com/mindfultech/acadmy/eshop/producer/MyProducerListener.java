//package com.mindfultech.acadmy.eshop.producer;
//
//import com.mindfultech.acadmy.eshop.model.Order;
//import jakarta.annotation.Nullable;
//import org.apache.kafka.clients.producer.ProducerRecord;
//import org.apache.kafka.clients.producer.RecordMetadata;
//import org.springframework.kafka.support.ProducerListener;
//import org.springframework.stereotype.Component;
//
//
//@Component
//public class MyProducerListener implements ProducerListener<String, Order> {
//
//    @Override
//    public void onSuccess(ProducerRecord<String, Order> producerRecord, RecordMetadata recordMetadata) {
//        System.out.println("Message with key=" + producerRecord.key() + " sent successfully to partition "
//                + recordMetadata.partition() + " at offset " + recordMetadata.offset());
//    }
//
//    @Override
//    public void onError(ProducerRecord<String, Order> producerRecord, @Nullable RecordMetadata recordMetadata, Exception exception) {
//        System.err.println("Failed to send message with key=" + producerRecord.key() + ": " + exception.getMessage());
//    }
//
//}
//
