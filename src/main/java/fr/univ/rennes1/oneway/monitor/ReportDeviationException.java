package fr.univ.rennes1.oneway.monitor;

import avro.monitor.commands.Deviation;

public class ReportDeviationException extends Exception {

	private Deviation deviation;

	public ReportDeviationException(Deviation deviation) {
		this.deviation = deviation;
	}

	public Deviation getDeviation() {
		return deviation;
	}

}
