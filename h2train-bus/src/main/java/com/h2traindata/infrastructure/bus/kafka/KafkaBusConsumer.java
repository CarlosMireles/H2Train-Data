package com.h2traindata.infrastructure.bus.kafka;

import com.h2traindata.bus.BusMessageHandler;
import com.h2traindata.bus.IncomingBusMessage;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(BusMessageHandler.class)
@ConditionalOnProperty(prefix = "app.bus.consumer", name = "type", havingValue = "kafka")
public class KafkaBusConsumer {

    private static final String SOURCE = "kafka";

    private final List<BusMessageHandler> handlers;

    public KafkaBusConsumer(List<BusMessageHandler> handlers) {
        this.handlers = List.copyOf(handlers);
    }

    @KafkaListener(
            topics = "${app.bus.consumer.kafka.topic:h2train.events.v1}",
            groupId = "${app.bus.consumer.kafka.group-id:h2train-consumer}",
            containerFactory = "h2trainBusKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record) {
        IncomingBusMessage message = new IncomingBusMessage(
                SOURCE,
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                record.value()
        );
        handlers.forEach(handler -> handler.handle(message));
    }
}
