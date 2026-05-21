package com.h2traindata.infrastructure.bus.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.h2traindata.domain.BusEventEnvelope;
import java.time.Duration;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
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

    @Test
    void usesGenericConsumerPropertiesForKafkaInputAdapter() {
        KafkaBusConsumerConfiguration consumerConfiguration = new KafkaBusConsumerConfiguration();
        KafkaBusConsumerProperties properties = new KafkaBusConsumerProperties();
        properties.setBootstrapServers("localhost:29092");
        properties.setGroupId("custom-group");
        properties.setClientId("custom-client");
        properties.setAutoOffsetReset("latest");

        Map<String, Object> config =
                ((DefaultKafkaConsumerFactory<String, String>)
                        consumerConfiguration.h2trainBusConsumerFactory(properties))
                        .getConfigurationProperties();

        assertEquals("localhost:29092", config.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals("custom-group", config.get(ConsumerConfig.GROUP_ID_CONFIG));
        assertEquals("custom-client", config.get(ConsumerConfig.CLIENT_ID_CONFIG));
        assertEquals("latest", config.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
    }
}
