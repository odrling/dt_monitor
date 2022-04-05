package test.kafka.test.kafka.bpmn;

import test.kafka.test.kafka.bpmn.avro.ElementEvent;

public interface Rule {

	boolean check(Monitor monitor, ElementEvent event);

}
