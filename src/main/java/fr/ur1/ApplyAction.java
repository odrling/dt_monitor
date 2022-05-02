package fr.ur1;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;

import test.kafka.test.kafka.bpmn.avro.Command;

@Path("/command")
public class ApplyAction {

    @OnOverflow(OnOverflow.Strategy.DROP)
    @Inject
    @Channel("model-feed")
    Emitter<Command> emitter;

    @POST
    public Command applyAction(Command command) {
        System.out.println(command);
        emitter.send(command);
        return command;
    }

}
