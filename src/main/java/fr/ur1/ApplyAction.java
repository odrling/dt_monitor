package fr.ur1;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import test.kafka.test.kafka.bpmn.Consumer;
import test.kafka.test.kafka.bpmn.Model;
import test.kafka.test.kafka.bpmn.avro.Command;
import test.kafka.test.kafka.bpmn.avro.ElementEvent;

@Path("/action")
public class ApplyAction {

    @Inject
    Model model;
    @Inject
    Consumer consumer;

    @POST
    public ElementEvent applyAction(ElementEvent event) {
        Command cmd = new Command();
        cmd.setCommand(event);
        synchronized (model) {
            model.handle(cmd);
        }
        return event;
    }

}
