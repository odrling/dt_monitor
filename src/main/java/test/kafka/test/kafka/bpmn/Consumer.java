package test.kafka.test.kafka.bpmn;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.locks.Lock;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.DocumentRoot;
import org.eclipse.bpmn2.util.Bpmn2ResourceFactoryImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceFactoryImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

import test.kafka.test.kafka.bpmn.avro.Command;

@Singleton
public class Consumer {

	@Inject
	Model model;

	private KafkaConsumer<byte[], byte[]> consumer;

	@PostConstruct
	public void init() {
		final Properties props = new Properties();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
		props.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer-group");

		final String topic = "my_topic11";

		consumer = new KafkaConsumer<>(props);
		TopicPartition tp = new TopicPartition(topic, 0);

		consumer.assign(Collections.singletonList(tp));
		consumer.seek(tp, 0);

		consumerLoop();
	}

	@PreDestroy
	public void close() {
		consumer.close();
	}

	public void consumerLoop() {
		final ConsumerRecords<byte[], byte[]> consumerRecords = consumer.poll(Duration.ofMillis(1000));

		if (consumerRecords.count() == 0) {
			return;
		}

		synchronized (model) {
			consumerRecords.forEach(record -> {
				try {
					Decoder decoder = DecoderFactory.get().binaryDecoder(record.value(), null);
					DatumReader<Command> reader = new SpecificDatumReader<>(Command.getClassSchema());

					Command cmd = reader.read(null, decoder);

					model.handle(cmd);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ReportDeviationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
		}

		consumer.commitAsync();
	}

	public static void read(String[] args) {
		ResourceFactoryImpl bpmnFactory = new Bpmn2ResourceFactoryImpl();

		if (!EPackage.Registry.INSTANCE.containsKey(Bpmn2Package.eNS_URI)) {
			EPackage.Registry.INSTANCE.put(Bpmn2Package.eNS_URI, Bpmn2Package.eINSTANCE);
		}
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("bpmn", bpmnFactory);
		ResourceSet rs = new ResourceSetImpl();

		URI inUri = URI
				.createURI("file:/home/odrling/eclipse-workspaces/gemoc-xbpmn/test.bpmn/examples/process_1.bpmn");
		Resource resource = rs.getResource(inUri, true);

		DocumentRoot root = (DocumentRoot) resource.getContents().get(0);
		System.out.println(root.eContents());
	}
}
