package fr.ur1;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import test.kafka.test.kafka.bpmn.Consumer;
import test.kafka.test.kafka.bpmn.EObjectNotFound;
import test.kafka.test.kafka.bpmn.Model;
import test.kafka.test.kafka.bpmn.Producer;
import test.kafka.test.kafka.bpmn.avro.Command;
import test.kafka.test.kafka.bpmn.avro.ElementEvent;

@Path("/action")
public class ApplyAction {

    @Inject
    Model model;

    @Inject
    Consumer consumer;

    @Inject
    Producer producer;

    @POST
    public ElementEvent applyAction(ElementEvent event) {
        Command cmd = new Command();
        cmd.setCommand(event);
        try {
            model.findID(event.getElementID());
            producer.sendCommand(cmd);
        } catch (EObjectNotFound e) {
            // failed to find node
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return event;
    }

}
