package	test.kafka.test.kafka.bpmn.statemachine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.bpmn2.Activity;
import org.eclipse.bpmn2.Event;
import org.eclipse.bpmn2.FlowNode;
import org.eclipse.bpmn2.Gateway;

public class DeviationException extends Exception {

	private Action action;
	private Set<State> missingStates;

	public DeviationException(Action action, Set<State> missingStates) {
		this.action = action;
		this.missingStates = missingStates;
	}

	public DeviationException(Action action) {
		this(action, new HashSet<>());
	}

	public Action getAction() {
		return this.action;
	}

	public Set<State> getMissingStates() {
		return this.missingStates;
	}

	public Map<FlowNode, String>  getRelatedNodes() {
		Map<FlowNode, String> relatedNodes = new HashMap<>();

		for (State state: this.missingStates) {
			String message;
			FlowNode node = state.getNode();
			if (node instanceof Gateway) {
				message = "Synchronization error";
			} else if (node instanceof Activity) {
				message = "Activity incomplete";
			} else if (node instanceof Event) {
				message = "Event incomplete";
			} else {
				message = "Undefined deviation";
			}
			relatedNodes.put(node, message);
		}

		return relatedNodes;
	}

}
