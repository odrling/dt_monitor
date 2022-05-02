package test.kafka.test.kafka.bpmn;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

import test.kafka.test.kafka.bpmn.avro.ElementEvent;
import test.kafka.test.kafka.bpmn.avro.action;

@Singleton
public class TimeMonitor {

	private Map<String, Long> startTimes;
	private Map<String, Long> startWaitingTime;

	public TimeMonitor() {
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

	public void monitor(ElementEvent event, Long timestamp) {
		if (event.getAction() == action.Start) {
			System.out.println("adding node ts " + event.getElementID());
			this.startWaitingTime.remove(event.getElementID());
			this.startTimes.put(event.getElementID(), timestamp);
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
