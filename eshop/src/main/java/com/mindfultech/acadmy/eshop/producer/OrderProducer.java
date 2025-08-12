package com.mindfultech.acadmy.eshop.producer;

import com.mindfultech.acadmy.eshop.config.KafkaConfig;
import com.mindfultech.acadmy.eshop.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

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
        kafkaTemplate.executeInTransaction(operations -> {
            CompletableFuture<SendResult<String, Order>> future1 = operations.send(KafkaConfig.TOPIC_NAME, order.getOrderId(), order);
            attachCallback(future1, order.getOrderId());
            return true;
        });
    }

    /**
     * Attach a callback to log success or failure for a send
     */
    private void attachCallback(CompletableFuture<SendResult<String, Order>> future, String key) {
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                logger.error("Failed to send order key={} : {}", key, ex.getMessage());
            } else if (result != null && result.getRecordMetadata() != null) {
                var md = result.getRecordMetadata();
                logger.info("Callback: Successfully sent order key={} to topic={} partition={} offset={}",
                        key, md.topic(), md.partition(), md.offset());
            } else {
                logger.info("Callback: Order key={} sent with no metadata", key);
            }
        });
    }

}
