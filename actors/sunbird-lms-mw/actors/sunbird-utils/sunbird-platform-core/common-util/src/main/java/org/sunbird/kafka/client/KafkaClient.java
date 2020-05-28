package org.sunbird.kafka.client;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;

/**
 * Helper class for creating a Kafka consumer and producer.
 *
 * @author Pradyumna
 */
public class KafkaClient {

  private static final String BOOTSTRAP_SERVERS = ProjectUtil.getConfigValue("kafka_urls");
  private static Producer<String, String> producer;
  private static Consumer<String, String> consumer;
  private static volatile Map<String, List<PartitionInfo>> topics;

  static {
    loadProducerProperties();
    loadConsumerProperties();
    loadTopics();
  }

  private static void loadProducerProperties() {
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
    props.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaClientProducer");
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.put(ProducerConfig.LINGER_MS_CONFIG, ProjectUtil.getConfigValue("kafka_linger_ms"));
    producer = new KafkaProducer<String, String>(props);
  }

  private static void loadTopics() {
    if (consumer == null) {
      loadConsumerProperties();
    }
    topics = consumer.listTopics();
    ProjectLogger.log(
        "KafkaClient:loadTopics Kafka topic infos =>" + topics, LoggerEnum.INFO.name());
  }

  private static void loadConsumerProperties() {
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
    props.put(ConsumerConfig.CLIENT_ID_CONFIG, "KafkaClientConsumer");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    consumer = new KafkaConsumer<>(props);
  }

  public static Producer<String, String> getProducer() {
    return producer;
  }

  public static Consumer<String, String> getConsumer() {
    return consumer;
  }

  public static void send(String event, String topic) throws Exception {
    if (validate(topic)) {
      final Producer<String, String> producer = getProducer();
      ProducerRecord<String, String> record = new ProducerRecord<String, String>(topic, event);
      producer.send(record);
    } else {
      ProjectLogger.log("Topic id: " + topic + ", does not exists.", LoggerEnum.ERROR);
      throw new ProjectCommonException(
          "TOPIC_NOT_EXISTS_EXCEPTION",
          "Topic id: " + topic + ", does not exists.",
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  public static void send(String key, String event, String topic) throws Exception {
    if (validate(topic)) {
      final Producer<String, String> producer = getProducer();
      ProducerRecord<String, String> record = new ProducerRecord<String, String>(topic, key, event);
      producer.send(record);
    } else {
      ProjectLogger.log("Topic id: " + topic + ", does not exists.", LoggerEnum.ERROR);
      throw new ProjectCommonException(
          "TOPIC_NOT_EXISTS_EXCEPTION",
          "Topic id: " + topic + ", does not exists.",
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  private static boolean validate(String topic) throws Exception {
    if (topics == null) {
      loadTopics();
    }
    return topics.keySet().contains(topic);
  }
}
