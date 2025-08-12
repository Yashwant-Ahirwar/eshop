package com.mindfultech.acadmy.eshop.config;

import com.mindfultech.acadmy.eshop.model.Order;
import com.mindfultech.acadmy.eshop.model.OrderSerializer;
import com.mindfultech.acadmy.eshop.model.OrderDeserializer;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.transaction.KafkaTransactionManager;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    public static final String TOPIC_NAME = "orders";

    @Bean
    public ProducerFactory<String, Order> producerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, OrderSerializer.class);
        // Reliability & retry tuning
        configs.put(ProducerConfig.ACKS_CONFIG, "all"); // strongest durability
        configs.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // avoid duplicates on retries
        configs.put(ProducerConfig.RETRIES_CONFIG, 5); // finite retries
        configs.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 100);
        configs.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // Timeouts
        configs.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30_000);
        configs.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 60_000);

        // Optional performance tuning
        configs.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        configs.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configs.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");

        DefaultKafkaProducerFactory<String,Order> pf = new DefaultKafkaProducerFactory<>(configs);
        // This enables transactions and the factory will create transactional.id = prefix + n
        pf.setTransactionIdPrefix("tx-");

        return pf;
    }

    @Bean
    @Primary
    public KafkaTemplate<String, Order> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, Order> consumerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configs.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, "order-group");
        configs.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, OrderDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(configs);
    }

    @Bean
    public NewTopic topic() {
        return new NewTopic(TOPIC_NAME, 1, (short) 1);
    }

    @Bean
    public NewTopic ordersDlqTopic() {
        return new NewTopic("orders.DLQ", 1, (short) 1);
    }

    @Bean
    public Serializer<Order> orderSerializer() {
        return new com.mindfultech.acadmy.eshop.model.OrderSerializer();
    }

    @Bean
    public Deserializer<Order> orderDeserializer() {
        return new com.mindfultech.acadmy.eshop.model.OrderDeserializer();
    }

    @Bean
    public ApplicationRunner showProducerConfig(ProducerFactory<String, Order> pf) {
        return args -> {
            try {
                Map<String, Object> cfg = pf.getConfigurationProperties();
                System.out.println("Effective Producer configs:");
                cfg.forEach((k,v) -> System.out.println(k + " = " + v));
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    @Bean
    public KafkaTransactionManager<String,Order> kafkaTransactionManager(ProducerFactory<String,Order> pf) {
        return new KafkaTransactionManager<>(pf);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String,Order> kafkaListenerContainerFactory(
            ConsumerFactory<String,Order> cf,
            KafkaTransactionManager<String,Order> kafkaTransactionManager,
            DefaultErrorHandler errorHandler) {

        ConcurrentKafkaListenerContainerFactory<String,Order> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(cf);
        factory.setCommonErrorHandler(errorHandler);

        // Link the KafkaAwareTransactionManager to the container so it starts transactions,
        // and offsets will be sent as part of the transaction (commit on tx commit).
        factory.getContainerProperties().setKafkaAwareTransactionManager(kafkaTransactionManager);

        // concurrency, poll timeout, etc. as needed
        factory.setConcurrency(3);

        return factory;
    }

    @Bean
    public ProducerFactory<String, String> nonTxProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());

        // Remove transactionIdPrefix if exists
        props.remove(ProducerConfig.TRANSACTIONAL_ID_CONFIG);

        return new DefaultKafkaProducerFactory<>(props);
    }
    @Bean
    public KafkaTemplate<String,Order> dlqKafkaTemplate(ProducerFactory<String,Order> pf) {
        return new KafkaTemplate<>(pf);
    }
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaOperations dlqTemplate) {
        return new DeadLetterPublishingRecoverer(dlqTemplate, (rec, ex) -> new TopicPartition(rec.topic() + ".DLQ", rec.partition()));
    }

    @Bean
    public DefaultErrorHandler defaultErrorHandler() {
        // Retry 3 times, with exponential backoff (starting at 1s, doubling each time)
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10000L);

        return new DefaultErrorHandler(backOff);
    }

}
