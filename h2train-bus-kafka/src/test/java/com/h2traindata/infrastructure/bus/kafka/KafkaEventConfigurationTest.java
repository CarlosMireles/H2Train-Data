package com.h2traindata.infrastructure.bus.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.h2traindata.domain.BusEventEnvelope;
import java.time.Duration;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;

class KafkaEventConfigurationTest {

    private final KafkaEventConfiguration configuration = new KafkaEventConfiguration();

    @Test
    void usesKafkaExpectedTypesForProducerTimeouts() {
        KafkaEventProperties properties = new KafkaEventProperties();
        properties.setRequestTimeout(Duration.ofSeconds(10));
        properties.setDeliveryTimeout(Duration.ofSeconds(20));
        properties.setMaxBlock(Duration.ofSeconds(5));

        ProducerFactory<String, BusEventEnvelope> producerFactory =
                configuration.h2trainEventProducerFactory(properties);

        Map<String, Object> config =
                ((DefaultKafkaProducerFactory<String, BusEventEnvelope>) producerFactory).getConfigurationProperties();

        assertEquals(10_000, config.get(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG));
        assertEquals(20_000, config.get(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG));
        assertEquals(5_000L, config.get(ProducerConfig.MAX_BLOCK_MS_CONFIG));
    }
}
