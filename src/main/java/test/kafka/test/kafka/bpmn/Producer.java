package test.kafka.test.kafka.bpmn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicExistsException;
import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.DocumentRoot;
import org.eclipse.bpmn2.util.Bpmn2ResourceFactoryImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceFactoryImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

public class Producer {

	final static ObjectMapper mapper = new ObjectMapper();

	public static void main(String[] args) {
		try {
			String payload = getXMIData();

			final Properties props = new Properties();
			props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
			props.put(ProducerConfig.ACKS_CONFIG, "all");
			props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
					"org.apache.kafka.common.serialization.StringSerializer");
			props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
					"org.apache.kafka.common.serialization.StringSerializer");

			String topic = "test_topic_bpmn";
			createTopic(topic, props);

			DataRecord d = new SetXMIDataRecord(payload);
			String s = mapper.writeValueAsString(d.toJson());

			KafkaProducer<String, String> producer = new KafkaProducer<>(props);
			ProducerRecord<String, String> r = new ProducerRecord<>(topic, s);

			producer.send(r);

			producer.close();
			System.out.println("done");
		} catch (IOException e) {
			e.printStackTrace();
		}
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

	public static DocumentRoot getModel() {
		ResourceFactoryImpl bpmnFactory = new Bpmn2ResourceFactoryImpl();

		if (!EPackage.Registry.INSTANCE.containsKey(Bpmn2Package.eNS_URI)) {
			EPackage.Registry.INSTANCE.put(Bpmn2Package.eNS_URI, Bpmn2Package.eINSTANCE);
		}
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("bpmn", bpmnFactory);
		ResourceSet rs = new ResourceSetImpl();
		URI inUri = URI
				.createURI("file:/home/odrling/eclipse-workspaces/gemoc-xbpmn/test.bpmn/examples/process_1.bpmn");
		Resource resource = rs.getResource(inUri, true);

		// JsonResourceFactory factory = new JsonResourceFactory(mapper);
		DocumentRoot root = (DocumentRoot) resource.getContents().get(0);
		return root;
	}

	public static String getXMIData() throws IOException {
		File f = new File("/home/odrling/eclipse-workspaces/gemoc-xbpmn/test.bpmn/examples/process_1.bpmn");
		String out = "";

		try (BufferedReader in = new BufferedReader(new FileReader(f))) {
			String line;

			while ((line = in.readLine()) != null) {
				out += line;
			}
		}

		// System.out.println(out);

		return out;
	}

}
