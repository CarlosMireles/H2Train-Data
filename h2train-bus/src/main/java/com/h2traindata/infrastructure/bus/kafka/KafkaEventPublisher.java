package com.h2traindata.infrastructure.bus.kafka;

import com.h2traindata.bus.EventPublisher;
import com.h2traindata.domain.BusEventEnvelope;
import com.h2traindata.domain.EventPublication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(prefix = "app.bus", name = "type", havingValue = "kafka")
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, BusEventEnvelope> kafkaTemplate;
    private final KafkaEventProperties properties;

    public KafkaEventPublisher(KafkaTemplate<String, BusEventEnvelope> kafkaTemplate,
                               KafkaEventProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Override
    public void publish(EventPublication publication) {
        BusEventEnvelope envelope = BusEventEnvelope.from(publication);
        String key = messageKey(envelope);
        try {
            kafkaTemplate.send(properties.getTopic(), key, envelope).join();
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Failed to publish event to Kafka topic " + properties.getTopic(), exception);
        }
    }

    private String messageKey(BusEventEnvelope envelope) {
        if (StringUtils.hasText(envelope.userId())) {
            return envelope.userId();
        }
        return envelope.providerId() + ":" + envelope.athleteId();
    }
}
