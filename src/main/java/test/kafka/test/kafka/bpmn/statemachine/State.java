package test.kafka.test.kafka.bpmn.statemachine;

import org.eclipse.bpmn2.FlowNode;

public class State {
	private FlowNode node;
	private Status status;

	public State(FlowNode node, Status status) {
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
}
