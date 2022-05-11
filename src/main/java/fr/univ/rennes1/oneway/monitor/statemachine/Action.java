package fr.univ.rennes1.oneway.monitor.statemachine;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.bpmn2.FlowNode;

public class Action {
	private FlowNode flowNode;
	private Status status;

	private static Map<FlowNode, Map<Status, Action>> actions = new HashMap<>();

	private Action(FlowNode flowNode, Status status) {
		this.flowNode = flowNode;
		this.setStatus(status);
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public FlowNode getNode() {
		return flowNode;
	}

	public void setFlowNode(FlowNode flowNode) {
		this.flowNode = flowNode;
	}

	public static Action get(FlowNode node, Status status) {
		Map<Status, Action> nodeActions;

		if (!(actions.containsKey(node)))
			actions.put(node, new HashMap<Status, Action>());

		nodeActions = actions.get(node);

		if (!(nodeActions.containsKey(status)))
			nodeActions.put(status, new Action(node, status));

		return nodeActions.get(status);
	}

	public String toString() {
		return "<Action " + this.flowNode.getId() + " " + this.status + ">";
	}
}
