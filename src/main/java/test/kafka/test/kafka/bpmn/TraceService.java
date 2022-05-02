package test.kafka.test.kafka.bpmn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;

import test.kafka.test.kafka.bpmn.avro.Command;

@Singleton
public class TraceService {

	final static ObjectMapper mapper = new ObjectMapper();

	public void save(Command command) throws IOException {
		File file = new File("model-trace");
		file.createNewFile();

		ObjectOutputStream oStream = new ObjectOutputStream(new FileOutputStream(file, true));

		try (oStream) {
			command.writeExternal(oStream);
		}
	}

	public List<Command> playback() throws FileNotFoundException, IOException, ClassNotFoundException {
		List<Command> commands = new LinkedList<>();
		File file = new File("model-trace");
		if (!file.exists()) {
			return commands;
		}

		ObjectInputStream iStream = new ObjectInputStream(new FileInputStream(file));
		try (iStream) {
			while (iStream.available() > 0) {
				Command command = new Command();
				command.readExternal(iStream);
				commands.add(command);
			}
		}

		return commands;
	}

}
