package test.kafka.test.kafka.bpmn;

import test.kafka.test.kafka.bpmn.avro.ElementEvent;

public class FlowNodeSequenceRule implements Rule {

	public boolean check(Monitor monitor, ElementEvent event) {
		return monitor.isAllowedAction(event);
	}

}
