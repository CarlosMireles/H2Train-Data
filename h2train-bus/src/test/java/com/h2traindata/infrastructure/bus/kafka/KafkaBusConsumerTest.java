package com.h2traindata.infrastructure.bus.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.h2traindata.bus.BusMessageHandler;
import com.h2traindata.bus.IncomingBusMessage;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class KafkaBusConsumerTest {

    @Test
    void adaptsKafkaRecordToGenericIncomingBusMessage() {
        BusMessageHandler handler = mock(BusMessageHandler.class);
        KafkaBusConsumer consumer = new KafkaBusConsumer(List.of(handler));
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "h2train.events.v1",
                2,
                42L,
                "message-key",
                "{\"messageId\":\"message-1\"}"
        );

        consumer.consume(record);

        ArgumentCaptor<IncomingBusMessage> captor = ArgumentCaptor.forClass(IncomingBusMessage.class);
        verify(handler).handle(captor.capture());
        IncomingBusMessage message = captor.getValue();
        assertEquals("kafka", message.source());
        assertEquals("h2train.events.v1", message.channel());
        assertEquals(2, message.partition());
        assertEquals(42L, message.offset());
        assertEquals("message-key", message.key());
        assertEquals("{\"messageId\":\"message-1\"}", message.payload());
    }
}
