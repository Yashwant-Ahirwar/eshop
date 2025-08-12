package com.mindfultech.acadmy.eshop.producer;

import com.mindfultech.acadmy.eshop.config.KafkaConfig;
import com.mindfultech.acadmy.eshop.model.Order;
import org.apache.kafka.common.errors.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class OrderProducer {

    private static final Logger logger = LoggerFactory.getLogger(OrderProducer.class);

    private final KafkaTemplate<String, Order> kafkaTemplate;

    @Autowired
    public OrderProducer(KafkaTemplate<String, Order> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

//    public void sendOrder(Order order) {
//        kafkaTemplate.send("orders", order.getOrderId(), order);
//    }

    public void sendOrder(Order order) {
        kafkaTemplate.executeInTransaction(status -> {
            try {
                // Send main message
                var future = kafkaTemplate.send(KafkaConfig.TOPIC_NAME, order.getOrderId(), order);
                // Now, attach handlers
                future.thenAccept(result -> {
                    // success handling
                    if (result != null && result.getRecordMetadata() != null) {
                        var md = result.getRecordMetadata();
                        logger.info("Order {} sent successfully to topic={} partition={} offset={}",
                                order.getOrderId(), md.topic(), md.partition(), md.offset());
                    } else {
                        logger.info("Order {} sent but no metadata", order.getOrderId());
                    }
                }).exceptionally(ex -> {
                    // send failed, forward to DLQ
                    handleSendFailure(order, ex);
                    return null;
                });
                // Move on or wait? For async, just return true (not wait)
                return true;
            } catch (Exception e) {
                // If any exception arises outside send, rollback
                logger.error("Error during transaction for order {}: {}", order.getOrderId(), e.getMessage(), e);
                throw e; // will cause rollback
            }
        });
    }

    private void handleSendFailure(Order order, Throwable ex) {
        logger.error("Send failed for order {}: {}", order.getOrderId(), ex.getMessage(), ex);
        // Forward to DLQ synchronously with timeout
        try {
            logger.info("Forwarding order {} to DLQ topic={}", order.getOrderId(), KafkaConfig.DLQ_TOPIC);
            kafkaTemplate.send(KafkaConfig.DLQ_TOPIC, order.getOrderId(), order).get(5, TimeUnit.SECONDS);
            logger.info("Order {} forwarded to DLQ", order.getOrderId());
        } catch (TimeoutException te) {
            logger.error("Timeout forwarding order {} to DLQ: {}", order.getOrderId(), te.getMessage(), te);
        } catch (ExecutionException ee) {
            logger.error("Execution exception forwarding order {} to DLQ: {}", order.getOrderId(), ee.getMessage(), ee);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted forwarding order {} to DLQ: {}", order.getOrderId(), ie.getMessage(), ie);
        } catch (CancellationException ce) {
            logger.error("Cancel forwarding order {} to DLQ: {}", order.getOrderId(), ce.getMessage(), ce);
        } catch (Exception e) {
            logger.error("Unexpected error forwarding order {} to DLQ: {}", order.getOrderId(), e.getMessage(), e);
        }
    }

}
