package test.kafka.test.kafka.bpmn;

import org.eclipse.bpmn2.DocumentRoot;
import org.eclipse.bpmn2.FlowNode;
import org.eclipse.emf.ecore.EObject;

import test.kafka.test.kafka.bpmn.avro.ElementEvent;
import test.kafka.test.kafka.bpmn.avro.action;
import test.kafka.test.kafka.bpmn.statemachine.Action;
import test.kafka.test.kafka.bpmn.statemachine.State;
import test.kafka.test.kafka.bpmn.statemachine.StateMachine;
import test.kafka.test.kafka.bpmn.statemachine.Status;

public class Monitor {

	private DocumentRoot root;
	private StateMachine stateMachine;

	public Monitor(DocumentRoot root) {
		this.root = root;
		this.stateMachine = StateMachine.fromBPMNRoot(root);
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
