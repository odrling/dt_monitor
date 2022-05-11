package fr.univ.rennes1.oneway.monitor.statemachine;

public class Transition {
	private Action action;
	private State source;
	private State target;
	private int outTokens;
	private int inTokens;

	public Transition(Action action, State source, State target, int inTokens, int outTokens) {
		this.action = action;
		this.source = source;
		this.target = target;
		this.inTokens = inTokens;
		this.outTokens = outTokens;
	}

	public Transition(Action action, State source, State target) {
		this(action, source, target, 1, 1);
	}

	public int getInTokens() {
		return inTokens;
	}

	public int getOutTokens() {
		return outTokens;
	}

	public State getTarget() {
		return target;
	}

	public State getSource() {
		return source;
	}

	public Action getAction() {
		return action;
	}

}
