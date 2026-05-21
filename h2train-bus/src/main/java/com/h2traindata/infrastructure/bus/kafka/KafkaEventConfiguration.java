package com.h2traindata.infrastructure.bus.kafka;

import com.h2traindata.domain.BusEventEnvelope;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
@EnableConfigurationProperties(KafkaEventProperties.class)
@ConditionalOnProperty(prefix = "app.bus", name = "type", havingValue = "kafka")
public class KafkaEventConfiguration {

    @Bean
    public KafkaAdmin h2trainKafkaAdmin(KafkaEventProperties properties) {
        Map<String, Object> config = new HashMap<>();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        config.put(AdminClientConfig.CLIENT_ID_CONFIG, properties.getClientId() + "-admin");
        KafkaAdmin kafkaAdmin = new KafkaAdmin(config);
        kafkaAdmin.setOperationTimeout((int) properties.getRequestTimeout().toSeconds());
        return kafkaAdmin;
    }

    @Bean
    public NewTopic h2trainEventsTopic(KafkaEventProperties properties) {
        return TopicBuilder.name(properties.getTopic())
                .partitions(properties.getTopicPartitions())
                .replicas(properties.getTopicReplicationFactor())
                .build();
    }

    @Bean
    public ProducerFactory<String, BusEventEnvelope> h2trainEventProducerFactory(KafkaEventProperties properties) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        config.put(ProducerConfig.CLIENT_ID_CONFIG, properties.getClientId());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, intMillis(properties.getRequestTimeout()));
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, intMillis(properties.getDeliveryTimeout()));
        config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, properties.getMaxBlock().toMillis());
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, BusEventEnvelope> h2trainEventKafkaTemplate(
            ProducerFactory<String, BusEventEnvelope> h2trainEventProducerFactory
    ) {
        return new KafkaTemplate<>(h2trainEventProducerFactory);
    }

    private int intMillis(java.time.Duration duration) {
        return Math.toIntExact(duration.toMillis());
    }
}
