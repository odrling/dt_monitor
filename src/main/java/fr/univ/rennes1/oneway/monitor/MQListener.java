package fr.univ.rennes1.oneway.monitor;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import avro.monitor.commands.Command;
import avro.monitor.commands.ElementEvent;
import avro.monitor.commands.StartProcess;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class MQListener {

	@Inject
	ModelRunner modelRunner;
	@Inject
	TraceService traceService;

	@Incoming("modelInputSetModel")
	public void setModel(JsonObject setModelJsonObject) {
		try {
			StartProcess setModelData = setModelJsonObject.mapTo(StartProcess.class);
			System.out.println(setModelData);
			modelRunner.setProcessStart(setModelData.getTimestamp());

			Command command = Command.newBuilder().setCommand(setModelData).build();
			try {
				traceService.save(command);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Incoming("modelInputEvent")
	public void commandInput(JsonObject eventJsonObject) {
		try {
			ElementEvent event = eventJsonObject.mapTo(ElementEvent.class);

			if (this.modelRunner.getRoot() == null) {
				this.modelRunner.setProcessStart(event.getTimestamp());
			}
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
