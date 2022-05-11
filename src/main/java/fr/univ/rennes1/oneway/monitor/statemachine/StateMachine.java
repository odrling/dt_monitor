package fr.univ.rennes1.oneway.monitor.statemachine;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
import org.eclipse.bpmn2.Activity;
import org.eclipse.bpmn2.DocumentRoot;
import org.eclipse.bpmn2.EndEvent;
import org.eclipse.bpmn2.FlowNode;
import org.eclipse.bpmn2.Gateway;
import org.eclipse.bpmn2.SequenceFlow;
import org.eclipse.emf.ecore.EObject;

public class StateMachine {
	private Map<Action, Map<State, Map<State, Transition>>> transitions;
	private List<State> initialStates;
	private Set<State> endStates;
	private Bag<State> activeStates;

	public Map<Action, Map<State, Map<State, Transition>>> getTransitions() {
		return this.transitions;
	}

	public Bag<State> getActiveStates() {
		return activeStates;
	}

	public List<State> getInitialStates() {
		return initialStates;
	}

	public StateMachine(Map<Action, Map<State, Map<State, Transition>>> transitions, List<State> initialStates,
			Collection<State> endStates) {
		this.transitions = transitions;
		this.initialStates = initialStates;
		this.endStates = new HashSet<>(endStates);
		this.activeStates = new HashBag<>(initialStates);
	}

	public boolean isDone() {
		for (State endState : this.endStates) {
			if (this.activeStates.contains(endState)) {
				return true;
			}
		}
		return false;
	}

	public boolean isAllowed(Action action) {
		if (!transitions.containsKey(action)) {
			return false;
		}

		Map<State, Map<State, Transition>> actionTransitions = new HashMap<>(transitions.get(action));
		Set<State> transitionStates = new HashSet<>(this.activeStates);
		transitionStates.retainAll(actionTransitions.keySet());

		for (State state: transitionStates) {
			for (Transition t : actionTransitions.get(state).values()) {
				return true;
			}
		}

		return false;
	}

	synchronized public void applyAction(Action action) throws DeviationException {
		System.out.println(action);
		// System.out.println(this.activeStates);
		if (!transitions.containsKey(action)) {
			throw new DeviationException(action);
		}

		Map<State, Map<State, Transition>> actionTransitions = new HashMap<>(transitions.get(action));
		Set<State> transitionStates = new HashSet<>(this.activeStates);
		transitionStates.retainAll(actionTransitions.keySet());

		List<Transition> doneTransitions = new LinkedList<>();
		Set<Transition> toDoTransitions = new HashSet<>();

		for (State state: transitionStates) {
			for (Transition transition : actionTransitions.get(state).values()) {
				toDoTransitions.add(transition);
			}
		}

		if (transitionStates.isEmpty()) {
			throw new DeviationException(action, actionTransitions.keySet());
		}

		while (!toDoTransitions.isEmpty()) {
			for (Transition transition : toDoTransitions) {
				this.applyTransition(transition);
				doneTransitions.add(transition);
			}

			if (transitions.containsKey(Action.get(null, null))) {
				actionTransitions = transitions.get(Action.get(null, null));
				// System.out.println("null trans: " + actionTransitions);
				transitionStates = new HashSet<>(this.activeStates);
				transitionStates.retainAll(actionTransitions.keySet());

				toDoTransitions.clear();
				for (State state: transitionStates) {
					for (Transition transition : actionTransitions.get(state).values()) {
						toDoTransitions.add(transition);
					}
				}
				toDoTransitions.removeAll(doneTransitions);
			} else {
				// System.out.println("no null trans");
				break;
			}
		}
	}

	private void applyTransition(Transition transition) {
		if (this.activeStates.getCount(transition.getSource()) < transition.getInTokens())
			return;
		this.activeStates.remove(transition.getSource(), transition.getInTokens());
		this.activeStates.add(transition.getTarget(), transition.getOutTokens());
	}

	public StateMachine() {
		this(new HashMap<>(), new LinkedList<>(), new HashSet<>());
	}

	public static StateMachine fromBPMNRoot(DocumentRoot root) {
		List<FlowNode> initialNodes = findInitialNodes(root);
		Map<Action, Map<State, Map<State, Transition>>> transitions = new HashMap<>();
		Map<FlowNode, Map<Status, State>> states = new HashMap<>();
		List<State> initialStates = new LinkedList<>();
		Set<State> endStates = new HashSet<>();

		List<FlowNode> nodesToExplore = new LinkedList<>(initialNodes);
		while (!nodesToExplore.isEmpty()) {
			FlowNode node = nodesToExplore.remove(0);
			states.put(node, new HashMap<>());
			states.get(node).put(Status.COMPLETED, State.get(node, Status.COMPLETED));

			if (node instanceof Activity) {
				states.get(node).put(Status.ACTIVE, State.get(node, Status.ACTIVE));
				Action action = Action.get(node, Status.COMPLETED);
				Transition transition = new Transition(
						action, states.get(node).get(Status.ACTIVE), states.get(node).get(Status.COMPLETED));
				addTransition(transitions, transition);

				if (node.getIncoming().isEmpty()) {
					initialStates.add(states.get(node).get(Status.ACTIVE));
				} else {
					action = Action.get(node, Status.ACTIVE);
					transition = new Transition(
							action, states.get(node.getIncoming().get(0).getSourceRef()).get(Status.COMPLETED),
							states.get(node).get(Status.ACTIVE));
					addTransition(transitions, transition);
				}
			} else if (node instanceof Gateway) {
				Gateway gateway = (Gateway) node;
				for (SequenceFlow sequenceFlow : gateway.getIncoming()) {
					FlowNode previousNode = sequenceFlow.getSourceRef();
					if (!states.containsKey(previousNode)) {
						states.remove(node);
						nodesToExplore.add(node); // re-evaluate node later
						continue;
					}
				}

				State inputState = State.get(gateway, Status.ACTIVE);
				Transition gatewayTransition = new Transition(Action.get(null, null), inputState, states.get(gateway).get(Status.COMPLETED), gateway.getIncoming().size(), gateway.getOutgoing().size());
				addTransition(transitions, gatewayTransition);

				for (SequenceFlow sequenceFlow : gateway.getIncoming()) {
					FlowNode previousNode = sequenceFlow.getSourceRef();
					Transition toInput = new Transition(Action.get(null, null), states.get(previousNode).get(Status.COMPLETED), inputState);
					addTransition(transitions, toInput);
				}

			} else {
				if (node.getIncoming().isEmpty()) {
					initialStates.add(states.get(node).get(Status.COMPLETED));
				} else {
					FlowNode previousNode = node.getIncoming().get(0).getSourceRef();
					Action action = Action.get(node, Status.COMPLETED);
					Transition transition = new Transition(action, states.get(previousNode).get(Status.COMPLETED),
							states.get(node).get(Status.COMPLETED));
					addTransition(transitions, transition);
				}
			}

			if (node instanceof EndEvent)
				endStates.add(states.get(node).get(Status.COMPLETED));

			for (SequenceFlow sequenceFlow : node.getOutgoing()) {
				FlowNode nextNode = sequenceFlow.getTargetRef();
				nodesToExplore.add(nextNode);
			}
		}

		return new StateMachine(transitions, initialStates, endStates);
	}

	public static void addTransition(Map<Action, Map<State, Map<State, Transition>>> transitions,
			Transition transition) {
		if (!(transitions.containsKey(transition.getAction())))
			transitions.put(transition.getAction(), new HashMap<>());

		Map<State, Map<State, Transition>> actionTransition = transitions.get(transition.getAction());
		if (!(actionTransition.containsKey(transition.getSource())))
			actionTransition.put(transition.getSource(), new HashMap<>());
		actionTransition.get(transition.getSource()).put(transition.getTarget(), transition);
	}

	public static List<FlowNode> findInitialNodes(EObject root) {
		LinkedList<FlowNode> initialNodes = new LinkedList<>();
		for (EObject obj : root.eContents()) {
			if (obj instanceof FlowNode) {
				FlowNode node = (FlowNode) obj;
				if (node.getIncoming().isEmpty()) {
					initialNodes.add(node);
				}
			} else {
				initialNodes.addAll(findInitialNodes(obj));
			}
		}

		return initialNodes;
	}

}
