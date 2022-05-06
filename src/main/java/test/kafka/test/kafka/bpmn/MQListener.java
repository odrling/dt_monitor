package test.kafka.test.kafka.bpmn;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import avro.monitor.commands.Command;
import avro.monitor.commands.ElementEvent;
import avro.monitor.commands.SetXMICommand;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class MQListener {

	@Inject ModelRunner modelRunner;
	@Inject TraceService traceService;

	@Incoming("modelInputSetModel")
	public void setModel(JsonObject setModelJsonObject) {
		SetXMICommand setModelData = setModelJsonObject.mapTo(SetXMICommand.class);
		System.out.println(setModelData);
		try {
			modelRunner.setXMI(setModelData);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Command command = Command.newBuilder().setCommand(setModelData).build();
		try {
			traceService.save(command);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Incoming("modelInputEvent")
	public void commandInput(JsonObject eventJsonObject) {
		ElementEvent event = eventJsonObject.mapTo(ElementEvent.class);
		System.out.println(event);
		try {
			modelRunner.handleEvent(event);
		} catch (ReportDeviationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Command command = Command.newBuilder().setCommand(event).build();
		try {
			traceService.save(command);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
