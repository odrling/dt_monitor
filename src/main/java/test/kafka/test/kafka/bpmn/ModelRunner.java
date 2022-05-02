package test.kafka.test.kafka.bpmn;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.DocumentRoot;
import org.eclipse.bpmn2.FlowNode;
import org.eclipse.bpmn2.util.Bpmn2ResourceFactoryImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceFactoryImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.kie.kogito.incubation.application.AppRoot;
import org.kie.kogito.incubation.common.ExtendedDataContext;
import org.kie.kogito.incubation.common.MapDataContext;
import org.kie.kogito.incubation.processes.LocalProcessId;
import org.kie.kogito.incubation.processes.ProcessIds;
import org.kie.kogito.incubation.processes.ProcessInstanceId;
import org.kie.kogito.incubation.processes.TaskInstanceId;
import org.kie.kogito.incubation.processes.services.StatefulProcessService;
import org.kie.kogito.incubation.processes.services.contexts.Policy;
import org.kie.kogito.incubation.processes.services.contexts.ProcessMetaDataContext;
import org.kie.kogito.incubation.processes.services.contexts.TaskMetaDataContext;
import org.kie.kogito.incubation.processes.services.contexts.TaskWorkItemDataContext;
import org.kie.kogito.incubation.processes.services.humantask.HumanTaskService;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.annotations.Blocking;
import test.kafka.test.kafka.bpmn.avro.Command;
import test.kafka.test.kafka.bpmn.avro.ElementEvent;
import test.kafka.test.kafka.bpmn.avro.SetXMICommand;
import test.kafka.test.kafka.bpmn.avro.action;

@Singleton
public class ModelRunner {

	private DocumentRoot root;

	private ExtendedDataContext process;

	@Inject
	TraceService traceService;

	@Inject
	TimeMonitor timeMonitor;

	@Inject
	AppRoot appRoot;

	@Inject
	ObjectMapper mapper;

	@Inject
	StatefulProcessService processSvc;

	@Inject
	HumanTaskService taskSvc;
	private ProcessInstanceId pid;

	public DocumentRoot getRoot() {
		return root;
	}

	public void setRoot(DocumentRoot root) {
		this.root = root;
	}

	@PostConstruct
	public void init() {
		this.root = null;

		try {
			for (Command cmd: traceService.playback()) {
				try {
					this.handle(cmd, false);
				} catch (ReportDeviationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private interface Handler {

		void handle(ModelRunner model, Object commandData, Long timestamp) throws ReportDeviationException;

	}

	private static final Map<Class<? extends Object>, Handler> dispatch = new HashMap<>();
	static {
		dispatch.put(SetXMICommand.class, new Handler() {
			@Override
			public void handle(ModelRunner model, Object cmdData, Long timestamp) {
				SetXMICommand commandData = (SetXMICommand) cmdData;
				try {
					model.setXMI(commandData.getSetXmi(), timestamp);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		dispatch.put(ElementEvent.class, new Handler() {
			@Override
			public void handle(ModelRunner model, Object cmdData, Long timestamp) throws ReportDeviationException {
				ElementEvent commandData = (ElementEvent) cmdData;

				if (commandData.getAction() != action.Start && commandData.getAction() != action.End) {
					throw new RuntimeException("invalid event status");
				}

				/// set policies for the task (as metadata)
				TaskMetaDataContext taskMeta = TaskMetaDataContext.of(Policy.of("admin", List.of("managers")));

				ExtendedDataContext tasks = model.taskSvc.get(model.pid.tasks(), taskMeta);
				List<TaskInstanceId> taskIdList = tasks.meta().as(TaskWorkItemDataContext.class).tasks();

				for (TaskInstanceId taskId: taskIdList) {
					System.out.println("task available: " + taskId.taskId().taskId());
					if (commandData.getElementID().equals(taskId.taskId().taskId())) {
						model.timeMonitor.monitor(commandData, timestamp);
						if (commandData.getAction() == action.End) {
							model.taskSvc.complete(taskId, MapDataContext.create());
						}
						return;
					}
				}

				// deviation
				System.out.println("DEVIATION: invalid action");
			}
		});
	}

	public void handle(Command cmd, boolean reportCommand) throws IOException, ReportDeviationException {
		Object commandData = cmd.getCommand();
		System.out.println("\nhandling " + commandData.getClass().getName());
		Handler handler = dispatch.get(commandData.getClass());
		try {
			handler.handle(this, commandData, cmd.getTimestamp());
		} catch (ReportDeviationException e) {
			if (reportCommand) {
				Command deviationCommand = Command.newBuilder().setCommand(e.getDeviation()).build();
				traceService.save(deviationCommand);

				// DeviationEvent deviationEvent = DeviationEvent.newBuilder().setEvent(e.getDeviation().getEvent()).build();
				// DeviationCommand devCmd = DeviationCommand.newBuilder()
				// 	.setDeviationID(e.getDeviation().getDeviationID())
				// 	.setModelTopic(this.producer.getTopic())
				// 	.setCommand(deviationEvent).build();
				// deviationProducer.sendCommand(devCmd);
			} else {
				throw e;
			}
		}
		if (reportCommand) {
			traceService.save(cmd);
		}

		System.out.println("done handling " + commandData.getClass().getName());
	}

	public void monitorWaitingTime(Long taskTime) {
		TaskMetaDataContext taskMeta = TaskMetaDataContext.of(Policy.of("admin", List.of("managers")));

		ExtendedDataContext tasks = this.taskSvc.get(this.pid.tasks(), taskMeta);
		List<TaskInstanceId> taskIdList = tasks.meta().as(TaskWorkItemDataContext.class).tasks();

		for (TaskInstanceId taskId: taskIdList) {
			this.timeMonitor.monitorWaiting(taskId.taskId().taskId(), taskTime);
		}
	}

	public void handle(Command cmd) throws ReportDeviationException {
		try {
			handle(cmd, false);
		} catch (IOException e) {
			// should not happen with reportCommand = false
			e.printStackTrace();
		}
	}

	public void setXMI(String modelXMI, Long timestamp) throws IOException {
		LocalProcessId id = appRoot.get(ProcessIds.class).get("process_1");
		MapDataContext ctx = mapper.readValue("{}", MapDataContext.class);

		this.process = processSvc.create(id, ctx);
		this.pid = this.process.meta().as(ProcessMetaDataContext.class).id(ProcessInstanceId.class);

		this.monitorWaitingTime(timestamp);

		ResourceFactoryImpl bpmnFactory = new Bpmn2ResourceFactoryImpl();

		if (!EPackage.Registry.INSTANCE.containsKey(Bpmn2Package.eNS_URI)) {
			EPackage.Registry.INSTANCE.put(Bpmn2Package.eNS_URI, Bpmn2Package.eINSTANCE);
		}
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("bpmn", bpmnFactory);
		ResourceSet rs = new ResourceSetImpl();
		URI inUri = URI.createURI("dummy:/model.bpmn");
		InputStream in = new ByteArrayInputStream(modelXMI.getBytes());
		Resource resource = rs.createResource(inUri);
		resource.load(in, rs.getLoadOptions());

		this.root = (DocumentRoot) resource.getContents().get(0);
		this.timeMonitor = new TimeMonitor();
	}

	public EObject findID(String id) throws EObjectNotFound {
		return findID(id, this.root);
	}

	public EObject findID(String id, EObject root) throws EObjectNotFound {
		for (EObject obj : root.eContents()) {
			if (obj instanceof FlowNode) {
				FlowNode node = (FlowNode) obj;
				if (node.getId().equals(id)) {
					return node;
				}
			}
			try {
				return findID(id, obj);
			} catch (EObjectNotFound e) {
			}
		}
		throw new EObjectNotFound(id);
	}

	@Incoming("model-input")
	@Blocking
	public void commandInput(Command command) throws IOException, ReportDeviationException {
		this.handle(command, true);
	}

}
