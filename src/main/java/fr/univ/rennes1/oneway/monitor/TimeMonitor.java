package fr.univ.rennes1.oneway.monitor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import avro.monitor.commands.ElementEvent;
import avro.monitor.commands.action;
import avro.monitor.state.TaskState;
import avro.monitor.state.state;
import avro.monitor.state.TaskState.Builder;

@ApplicationScoped
public class TimeMonitor {

	private volatile Map<String, TaskClock> startTimes;
	private volatile Map<String, TaskClock> startWaitingTime;

	@PostConstruct
	public void init() {
		this.startTimes = new HashMap<>();
		this.startWaitingTime = new HashMap<>();

		this.start();
	}

	public void checkTimeDeviation(TaskClock clock, String node, state currentState) {
		long duration = System.currentTimeMillis() - clock.getStartTime();

		if (!clock.isDone() && duration > getDuration(node)) {
			// TODO: produce deviation
			System.out.println("DEVIATION: time deviation on node (" + currentState + ") " + node);
		}
	}


	public List<TaskState> getTaskStates() {
		List<TaskState> taskStates = new LinkedList<>();
		for (String node: this.startWaitingTime.keySet()) {
			TaskClock taskClock = this.startWaitingTime.get(node);
			if (!taskClock.isDone()) {
				TaskState taskState = TaskState.newBuilder()
					.setElementID(node)
					.setState(state.Waiting)
					.build();

				taskStates.add(taskState);
			}
		}

		for (String node: this.startTimes.keySet()) {
			TaskClock taskClock = this.startTimes.get(node);

			TaskState taskState;
			Builder taskStateBuilder = TaskState.newBuilder().setElementID(node);
			if (taskClock.isDone()) {
				taskState = taskStateBuilder.setState(state.Completed).build();
			} else {
				taskState = taskStateBuilder.setState(state.Processing).build();
			}

			taskStates.add(taskState);
		}

		return taskStates;
	}

	public void start() {
		Thread t = new Thread(new Runnable() {
			public void run() {
				while (true) {
					for (String node: startTimes.keySet()) {
						TaskClock clock = startTimes.get(node);
						checkTimeDeviation(clock, node, state.Processing);
					}

					for (String node: startWaitingTime.keySet()) {
						TaskClock clock = startWaitingTime.get(node);
						checkTimeDeviation(clock, node, state.Waiting);
					}

					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		t.start();
	}

	public int getDuration(String nodeID) {
		return 15000; // TODO: get value from BPsim
	}

	public int getWaiting(String nodeID) {
		return 5000;
	}

	public synchronized void monitorWaiting(String node, Long timestamp) {
		if (!startWaitingTime.containsKey(node)) {
			TaskClock clock = new TaskClock(timestamp);
			this.startWaitingTime.put(node, clock);
		}
	}

	public synchronized void monitor(ElementEvent event) {
		if (event.getAction() == action.Start) {
			System.out.println("adding node ts " + event.getElementID());
			this.startWaitingTime.get(event.getElementID()).setEndTime(event.getTimestamp());

			TaskClock clock = new TaskClock(event.getTimestamp());
			this.startTimes.put(event.getElementID(), clock);
		} else { // End
			// assert timestamp - this.startTimes.get(node) < getDuration(node);
			if (this.startTimes.containsKey(event.getElementID())) {
				this.startTimes.get(event.getElementID()).setEndTime(event.getTimestamp());
			} else {
				throw new RuntimeException("DEVIATION: task " + event.getElementID() + " wasn't started");
			}
		}
	}

}
