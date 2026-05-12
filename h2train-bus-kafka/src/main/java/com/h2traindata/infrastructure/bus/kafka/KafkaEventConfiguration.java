package com.h2traindata.infrastructure.bus.kafka;

import com.h2traindata.domain.BusEventEnvelope;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
@EnableConfigurationProperties(KafkaEventProperties.class)
@ConditionalOnProperty(prefix = "app.bus", name = "type", havingValue = "kafka")
public class KafkaEventConfiguration {

    @Bean
    public ProducerFactory<String, BusEventEnvelope> h2trainEventProducerFactory(KafkaEventProperties properties) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        config.put(ProducerConfig.CLIENT_ID_CONFIG, properties.getClientId());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, BusEventEnvelope> h2trainEventKafkaTemplate(
            ProducerFactory<String, BusEventEnvelope> h2trainEventProducerFactory
    ) {
        return new KafkaTemplate<>(h2trainEventProducerFactory);
    }
}
