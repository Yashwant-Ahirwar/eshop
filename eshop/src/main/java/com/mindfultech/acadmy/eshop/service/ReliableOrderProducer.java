//package com.mindfultech.acadmy.eshop.service;
//
//import com.mindfultech.acadmy.eshop.model.Order;
//import com.mindfultech.acadmy.eshop.producer.MyProducerListener;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.retry.support.RetryTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//public class ReliableOrderProducer {
//
//    private final KafkaTemplate<String, Order> kafkaTemplate;
//    private final RetryTemplate retryTemplate; // Inject the RetryTemplate
//
//    @Autowired
//    public ReliableOrderProducer(KafkaTemplate<String, Order> kafkaTemplate,
//                                 MyProducerListener listener,
//                                 RetryTemplate retryTemplate) { // Inject RetryTemplate
//        this.kafkaTemplate = kafkaTemplate;
//        this.kafkaTemplate.setProducerListener(listener);
//        this.retryTemplate = retryTemplate;  // Store the RetryTemplate
//    }
//
//    public void sendOrder(Order order) {
//        try {
//            retryTemplate.execute(context -> {
//                // This code will be retried if it throws an exception
//                kafkaTemplate.executeInTransaction(operations -> {
//                    operations.send("orders", order.getOrderId(), order);
//                    return true;
//                });
//                return null; // Indicate success if the transaction completes
//            });
//        } catch (Exception e) {
//            // Handle the exception after all retries have failed.
//            // Log the error, throw a custom exception, etc.
//            System.err.println("Failed to send order after multiple retries: " + e.getMessage());
//            throw new OrderProducerException("Failed to send order after multiple retries", e);
//        }
//    }
//
//    // Custom Exception (Optional, but recommended):
//    public static class OrderProducerException extends RuntimeException {
//        public OrderProducerException(String message, Throwable cause) {
//            super(message, cause);
//        }
//    }
//}
//
//
