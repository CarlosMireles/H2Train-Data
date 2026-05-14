package com.h2traindata.datalake.kafka;

import com.h2traindata.datalake.application.DatalakeIngestionService;
import com.h2traindata.datalake.domain.IncomingBusMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.datalake.bus", name = "type", havingValue = "kafka", matchIfMissing = true)
public class DatalakeKafkaConsumer {

    private static final String SOURCE = "kafka";

    private final DatalakeIngestionService ingestionService;

    public DatalakeKafkaConsumer(DatalakeIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @KafkaListener(
            topics = "${app.datalake.kafka.topic:h2train.events.v1}",
            groupId = "${app.datalake.kafka.group-id:h2train-datalake}",
            containerFactory = "datalakeKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record) {
        ingestionService.ingest(new IncomingBusMessage(
                SOURCE,
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                record.value()
        ));
    }
}
