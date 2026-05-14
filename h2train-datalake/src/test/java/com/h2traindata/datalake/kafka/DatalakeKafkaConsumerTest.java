package com.h2traindata.datalake.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.h2traindata.datalake.application.DatalakeIngestionService;
import com.h2traindata.datalake.domain.IncomingBusMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DatalakeKafkaConsumerTest {

    @Test
    void adaptsKafkaRecordToGenericIncomingBusMessage() {
        DatalakeIngestionService ingestionService = mock(DatalakeIngestionService.class);
        DatalakeKafkaConsumer consumer = new DatalakeKafkaConsumer(ingestionService);
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "h2train.events.v1",
                2,
                42L,
                "message-key",
                "{\"messageId\":\"message-1\"}"
        );

        consumer.consume(record);

        ArgumentCaptor<IncomingBusMessage> captor = ArgumentCaptor.forClass(IncomingBusMessage.class);
        verify(ingestionService).ingest(captor.capture());
        IncomingBusMessage message = captor.getValue();
        assertEquals("kafka", message.source());
        assertEquals("h2train.events.v1", message.channel());
        assertEquals(2, message.partition());
        assertEquals(42L, message.offset());
        assertEquals("message-key", message.key());
        assertEquals("{\"messageId\":\"message-1\"}", message.payload());
    }
}
