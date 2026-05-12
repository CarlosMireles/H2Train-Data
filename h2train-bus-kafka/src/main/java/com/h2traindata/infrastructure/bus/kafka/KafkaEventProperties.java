package com.h2traindata.infrastructure.bus.kafka;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bus.kafka")
public class KafkaEventProperties {

    private String bootstrapServers = "localhost:9092";
    private String topic = "h2train.events.v1";
    private String clientId = "h2train-daemon";
    private int topicPartitions = 3;
    private short topicReplicationFactor = 1;
    private Duration requestTimeout = Duration.ofSeconds(10);
    private Duration deliveryTimeout = Duration.ofSeconds(20);
    private Duration maxBlock = Duration.ofSeconds(5);

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public int getTopicPartitions() {
        return topicPartitions;
    }

    public void setTopicPartitions(int topicPartitions) {
        this.topicPartitions = topicPartitions;
    }

    public short getTopicReplicationFactor() {
        return topicReplicationFactor;
    }

    public void setTopicReplicationFactor(short topicReplicationFactor) {
        this.topicReplicationFactor = topicReplicationFactor;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public Duration getDeliveryTimeout() {
        return deliveryTimeout;
    }

    public void setDeliveryTimeout(Duration deliveryTimeout) {
        this.deliveryTimeout = deliveryTimeout;
    }

    public Duration getMaxBlock() {
        return maxBlock;
    }

    public void setMaxBlock(Duration maxBlock) {
        this.maxBlock = maxBlock;
    }
}
