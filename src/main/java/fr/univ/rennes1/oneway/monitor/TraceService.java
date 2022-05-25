package fr.univ.rennes1.oneway.monitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import avro.monitor.commands.Command;
import avro.monitor.state.GlobalState;

@ApplicationScoped
@Path("/task_state")
public class TraceService {

	private Map<Long, GlobalState> states;
	
	@Inject
	TimeMonitor timeMonitor;

	private String currentBPMNXMI = null;

	public String getCurrentBPMNXMI() {
		return currentBPMNXMI;
	}

	public void setCurrentBPMNXMI(String currentBPMNXMI) {
		this.currentBPMNXMI = currentBPMNXMI;
	}

	public void saveState(Long timestamp) {
		System.out.println(timestamp);
		GlobalState globalState = this.getCurrentState();
		System.out.println(globalState);
		// this.states.put(timestamp, globalState);
	}

	public GlobalState getCurrentState() {
		return GlobalState.newBuilder()
			.setBpmnModel(this.currentBPMNXMI)
			.setTasks(timeMonitor.getTaskStates()).build();
	}

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

	@GET
	@Path("current")
	public GlobalState currentState() {
		return this.getCurrentState();
	}

}
