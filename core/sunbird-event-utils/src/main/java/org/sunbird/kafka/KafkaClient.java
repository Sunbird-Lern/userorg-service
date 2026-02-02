package org.sunbird.kafka;

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
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.common.ProjectUtil;
import org.sunbird.response.ResponseCode;

/**
 * Helper class for creating and managing Kafka consumers and producers.
 * <p>
 * This class supports two modes of operation:
 * 1. **Singleton Mode**: Provides a shared, lazily-initialized Producer string-key/string-value pairs.
 *    Suitable for general-purpose event logging where a single connection is sufficient.
 * 2. **Factory Mode**: Provides methods to create new Producer/Consumer instances with 
 *    custom configurations (Long-key/String-value).
 * </p>
 * 
 * @author Pradyumna
 */
public class KafkaClient {

  private static final LoggerUtil logger = new LoggerUtil(KafkaClient.class);
  private static final String BOOTSTRAP_SERVERS = ProjectUtil.getConfigValue("kafka_urls");
  private static Producer<String, String> producer;
  private static Consumer<String, String> consumer;
  private static volatile Map<String, List<PartitionInfo>> topics;

  static {
    loadProducerProperties();
    loadConsumerProperties();
    loadTopics();
  }

  // Singleton Methods

  /**
   * Retrieves the singleton Kafka Producer instance (String key, String value).
   * 
   * @return The singleton {@link Producer} instance.
   */
  public static Producer<String, String> getProducer() {
    return producer;
  }

  /**
   * Retrieves the singleton Kafka Consumer instance (String key, String value).
   * 
   * @return The singleton {@link Consumer} instance.
   */
  public static Consumer<String, String> getConsumer() {
    return consumer;
  }

  /**
   * Sends a message to a Kafka topic using the singleton producer.
   *
   * @param event The message content/payload.
   * @param topic The target Kafka topic name.
   * @throws Exception If the topic does not exist or if sending the message fails.
   */
  public static void send(String event, String topic) throws Exception {
    if (validate(topic)) {
      getProducer().send(new ProducerRecord<>(topic, event));
    } else {
      logger.info("KafkaClient:send: Topic id: " + topic + ", does not exist.");
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing.getErrorCode(),
          "Topic id: " + topic + ", does not exist.",
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  /**
   * Sends a message with a specific key to a Kafka topic using the singleton producer.
   *
   * @param key   The message key (used for partitioning).
   * @param event The message content/payload.
   * @param topic The target Kafka topic name.
   * @throws Exception If the topic does not exist or if sending the message fails.
   */
  public static void send(String key, String event, String topic) throws Exception {
    if (validate(topic)) {
      getProducer().send(new ProducerRecord<>(topic, key, event));
    } else {
      logger.info("KafkaClient:send: Topic id: " + topic + ", does not exist.");
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing.getErrorCode(),
          "Topic id: " + topic + ", does not exist.",
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  /**
   * Validates if a topic exists in the current Kafka cluster.
   * 
   * @param topic The topic name to check.
   * @return true if the topic exists, false otherwise.
   */
  private static boolean validate(String topic) {
    if (topics == null) {
      loadTopics();
    }
    return topics.keySet().contains(topic);
  }

  private static void loadProducerProperties() {
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
    props.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaClientProducer");
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.put(ProducerConfig.LINGER_MS_CONFIG, ProjectUtil.getConfigValue("kafka_linger_ms"));
    producer = new KafkaProducer<>(props);
  }

  private static void loadConsumerProperties() {
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
    props.put(ConsumerConfig.CLIENT_ID_CONFIG, "KafkaClientConsumer");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    consumer = new KafkaConsumer<>(props);
  }

  private static void loadTopics() {
    if (consumer == null) {
      loadConsumerProperties();
    }
    topics = consumer.listTopics();
    logger.info("KafkaClient:loadTopics: Kafka topic info => " + topics);
  }

  // Factory Methods

  /**
   * Creates a new Kafka Producer instance with Long keys and String values.
   * This is useful for scenarios requiring custom bootstrap servers or client IDs distinct from the singleton configuration.
   *
   * @param bootstrapServers Comma-separated list of Kafka broker addresses (e.g., "localhost:9092,host2:9092").
   * @param clientId         A unique identifier for this producer client.
   * @return A new {@link Producer} instance configured with LongSerializer for keys and StringSerializer for values.
   */
  public static Producer<Long, String> createProducer(String bootstrapServers, String clientId) {
    return new KafkaProducer<>(createProducerProperties(bootstrapServers, clientId));
  }

  /**
   * Creates a new Kafka Consumer instance with Long keys and String values.
   * This is useful for scenarios requiring custom bootstrap servers or client IDs distinct from the singleton configuration.
   *
   * @param bootstrapServers Comma-separated list of Kafka broker addresses (e.g., "localhost:9092,host2:9092").
   * @param clientId         A unique identifier for this consumer client.
   * @return A new {@link Consumer} instance configured with LongDeserializer for keys and StringDeserializer for values.
   */
  public static Consumer<Long, String> createConsumer(String bootstrapServers, String clientId) {
    return new KafkaConsumer<>(createConsumerProperties(bootstrapServers, clientId));
  }

  private static Properties createProducerProperties(String bootstrapServers, String clientId) {
    logger.info("KafkaClient:createProducerProperties: called with bootstrapServers = " + bootstrapServers + " clientId = " + clientId);
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    return props;
  }

  private static Properties createConsumerProperties(String bootstrapServers, String clientId) {
    logger.info("KafkaClient:createConsumerProperties: called with bootstrapServers = " + bootstrapServers + " clientId = " + clientId);
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getName());
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    return props;
  }
}
