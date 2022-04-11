package test.kafka.test.kafka.bpmn;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.bpmn2.DocumentRoot;
import org.eclipse.bpmn2.FlowNode;
import org.eclipse.emf.ecore.EObject;

import test.kafka.test.kafka.bpmn.avro.ElementEvent;
import test.kafka.test.kafka.bpmn.avro.action;
import test.kafka.test.kafka.bpmn.statemachine.Action;
import test.kafka.test.kafka.bpmn.statemachine.DeviationException;
import test.kafka.test.kafka.bpmn.statemachine.State;
import test.kafka.test.kafka.bpmn.statemachine.StateMachine;
import test.kafka.test.kafka.bpmn.statemachine.Status;

public class Monitor {

	private DocumentRoot root;
	private StateMachine stateMachine;
	private List<Rule> rules;
	private Map<FlowNode, Long> startTimes;

	public Monitor(DocumentRoot root) {
		this.root = root;
		this.stateMachine = StateMachine.fromBPMNRoot(root);
		this.rules = new LinkedList<>();
		this.rules.add(new FlowNodeSequenceRule());
		this.startTimes = new HashMap<>();

		Thread t = new Thread(new Runnable() {
			public void run() {
				while (true) {
					for (FlowNode node: startTimes.keySet()) {
						long duration = System.currentTimeMillis() - startTimes.get(node);
						if (duration > getDuration(node)) {
							// TODO: produce deviation
							throw new RuntimeException("time deviation on node " + node.getId());
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

	public boolean check(ElementEvent event) {
		for (Rule rule: this.rules) {
			if (!rule.check(this, event)) {
				return false;
			}
		}
		return true;
	}

	public int getDuration(FlowNode node) {
		return 5000; // TODO: get value from BPsim
	}

	public void monitor(ElementEvent event) {
		FlowNode node;
		try {
			node = findID(event.getElementID());

			if (event.getAction() == action.Start) {
				System.out.println("adding node ts " + node.getId());
				this.startTimes.put(node, event.getTimestamp());
			} else { // End
				// assert event.getTimestamp() - this.startTimes.get(node) < getDuration(node);
				this.startTimes.remove(node);
			}
		} catch (EObjectNotFound e) {
			e.printStackTrace();
			throw new RuntimeException("failed to monitor activity");
		}
	}

	public FlowNode findID(String id) throws EObjectNotFound {
		return findID(id, this.root);
	}

	public FlowNode findID(String id, EObject root) throws EObjectNotFound {
		for (EObject obj : root.eContents()) {
			if (obj instanceof FlowNode) {
				FlowNode node = (FlowNode) obj;
				if (node.getId().equals(id)) {
					return node;
				}
			}
			try {
				return findID(id, obj);
			} catch (EObjectNotFound e) {
			}
		}
		throw new EObjectNotFound(id);
	}

	public boolean isCompleted(FlowNode sourceNode) {
		return this.stateMachine.getActiveStates().contains(State.get(sourceNode, Status.COMPLETED));
	}

	public boolean isAllowedAction(ElementEvent event) {
		FlowNode node;
		Action act;

		try {
			node = findID(event.getElementID());
		} catch (EObjectNotFound e) {
			return false; // node doesn't exist
		}

		if (event.getAction() == action.Start) {
			act = Action.get(node, Status.ACTIVE);
		} else if (event.getAction() == action.End) {
			act = Action.get(node, Status.COMPLETED);
		} else {
			throw new RuntimeException("invalid event status");
		}

		return this.stateMachine.isAllowed(act);
	}

}
