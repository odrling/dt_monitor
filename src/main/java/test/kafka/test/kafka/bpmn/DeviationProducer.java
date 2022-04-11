package test.kafka.test.kafka.bpmn;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import javax.annotation.PreDestroy;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicExistsException;

import test.kafka.test.kafka.deviations.avro.DeviationCommand;

public class DeviationProducer {

	final static ObjectMapper mapper = new ObjectMapper();
	private KafkaProducer<byte[], byte[]> producer;
	private String topic;

	public DeviationProducer(String topic) {
		this.topic = topic;

		final Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
		props.put(ProducerConfig.ACKS_CONFIG, "all");
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
				"org.apache.kafka.common.serialization.ByteArraySerializer");
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
				"org.apache.kafka.common.serialization.ByteArraySerializer");

		// createTopic(topic, props);

		producer = new KafkaProducer<>(props);
	}

	public void sendCommand(DeviationCommand command) throws IOException {
		ByteArrayOutputStream oStream = new ByteArrayOutputStream();
		BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(oStream, null);
		DatumWriter<DeviationCommand> writer = new SpecificDatumWriter<>(DeviationCommand.getClassSchema());
		writer.write(command, encoder);

		ProducerRecord<byte[], byte[]> r = new ProducerRecord<>(this.topic, oStream.toByteArray());
		producer.send(r);
	}

	@PreDestroy
	public void close() {
		producer.close();
	}

	public static Properties loadConfig(String configFile) throws IOException {
		if (!Files.exists(Paths.get(configFile))) {
			throw new IOException(configFile + " not found.");
		}
		final Properties cfg = new Properties();
		try (InputStream inputStream = new FileInputStream(configFile)) {
			cfg.load(inputStream);
		}
		return cfg;
	}

	public static void createTopic(final String topic, final Properties cloudConfig) {
		final NewTopic newTopic = new NewTopic(topic, 1, (short) 1);
		try (final AdminClient adminClient = AdminClient.create(cloudConfig)) {
			adminClient.createTopics(Collections.singletonList(newTopic)).all().get();
		} catch (final InterruptedException | ExecutionException e) {
			// Ignore if TopicExistsException, which may be valid if topic exists
			if (!(e.getCause() instanceof TopicExistsException)) {
				throw new RuntimeException(e);
			}
		}
	}

}