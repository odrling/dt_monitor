package test.kafka.test.kafka.bpmn;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;

import avro.monitor.commands.ElementEvent;
import avro.monitor.commands.action;

@ApplicationScoped
public class TimeMonitor {

	private Map<String, Long> startTimes;
	private Map<String, Long> startWaitingTime;

	@PostConstruct
	public void init() {
		this.startTimes = new HashMap<>();
		this.startWaitingTime = new HashMap<>();

		this.start();
	}

	public void start() {
		Thread t = new Thread(new Runnable() {
			public void run() {
				while (true) {
					for (String node: startTimes.keySet()) {
						long duration = System.currentTimeMillis() - startTimes.get(node);
						if (duration > getDuration(node)) {
							// TODO: produce deviation
							System.out.println("DEVIATION: time deviation on node (processing) " + node);
						}
					}

					for (String node: startWaitingTime.keySet()) {
						long duration = System.currentTimeMillis() - startWaitingTime.get(node);
						if (duration > getDuration(node)) {
							// TODO: produce deviation
							System.out.println("DEVIATION: time deviation on node (waiting) " + node);
						}
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

	public void monitorWaiting(String node, Long timestamp) {
		if (!startWaitingTime.containsKey(node)) {
			this.startWaitingTime.put(node, timestamp);
		}
	}

	public void monitor(ElementEvent event) {
		if (event.getAction() == action.Start) {
			System.out.println("adding node ts " + event.getElementID());
			this.startWaitingTime.remove(event.getElementID());
			this.startTimes.put(event.getElementID(), event.getTimestamp());
		} else { // End
			// assert timestamp - this.startTimes.get(node) < getDuration(node);
			if (this.startTimes.containsKey(event.getElementID())) {
				this.startTimes.remove(event.getElementID());
			} else {
				throw new RuntimeException("DEVIATION: task " + event.getElementID() + " wasn't started");
			}
		}
	}

}
