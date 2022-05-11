package fr.univ.rennes1.oneway.monitor.statemachine;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.bpmn2.FlowNode;

public class State {
	private static Map<FlowNode, Map<Status, State>> states = new HashMap<>();
	private FlowNode node;
	private Status status;

	private State(FlowNode node, Status status) {
		this.node = node;
		this.status = status;
	}

	public Status getStatus() {
		return status;
	}

	public FlowNode getNode() {
		return node;
	}

	public String toString() {
		return "<State " + this.node.getId() + " " + this.status + ">";
	}

	public static State get(FlowNode node, Status status) {
		Map<Status, State> nodeActions;

		if (!(states.containsKey(node)))
			states.put(node, new HashMap<Status, State>());

		nodeActions = states.get(node);

		if (!(nodeActions.containsKey(status)))
			nodeActions.put(status, new State(node, status));

		return nodeActions.get(status);
	}
}
