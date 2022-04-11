package fr.ur1;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import test.kafka.test.kafka.bpmn.ModelRunner;
import test.kafka.test.kafka.bpmn.ReportDeviationException;
import test.kafka.test.kafka.bpmn.avro.Command;
import test.kafka.test.kafka.bpmn.avro.ElementEvent;

@Path("/action")
public class ApplyAction {

    @Inject
    ModelRunner model;

    @POST
    public ElementEvent applyAction(ElementEvent event) {
        Command cmd = new Command();
        cmd.setCommand(event);
        try {
            model.handle(cmd, true);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ReportDeviationException e) {
            // should not happen
            e.printStackTrace();
        }
        return event;
    }

}
