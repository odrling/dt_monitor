package fr.univ.rennes1.oneway.monitor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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

import avro.monitor.commands.ElementEvent;
import avro.monitor.commands.SetXMICommand;
import avro.monitor.commands.action;

@ApplicationScoped
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

		// try {
		// 	for (Command cmd: traceService.playback()) {
		// 		try {
		// 			this.handle(cmd, false);
		// 		} catch (ReportDeviationException e) {
		// 			// TODO Auto-generated catch block
		// 			e.printStackTrace();
		// 		} catch (IOException e) {
		// 			// TODO Auto-generated catch block
		// 			e.printStackTrace();
		// 		}
		// 	}
		// } catch (ClassNotFoundException | IOException e) {
		// 	// TODO Auto-generated catch block
		// 	e.printStackTrace();
		// }
	}

	public synchronized void handleEvent(ElementEvent commandData) throws ReportDeviationException {

		if (commandData.getAction() != action.Start && commandData.getAction() != action.End) {
			throw new RuntimeException("invalid event status");
		}

		/// set policies for the task (as metadata)
		TaskMetaDataContext taskMeta = TaskMetaDataContext.of(Policy.of("admin", List.of("managers")));

		System.out.println(this.taskSvc);
		System.out.println(this.pid);
		ExtendedDataContext tasks = this.taskSvc.get(this.pid.tasks(), taskMeta);
		List<TaskInstanceId> taskIdList = tasks.meta().as(TaskWorkItemDataContext.class).tasks();

		for (TaskInstanceId taskId: taskIdList) {
			if (commandData.getElementID().equals(taskId.taskId().taskId())) {
				this.timeMonitor.monitor(commandData);
				if (commandData.getAction() == action.End) {
					this.taskSvc.complete(taskId, MapDataContext.create());
				}
				this.monitorWaitingTime(commandData.getTimestamp());
				return;
			}
		}

		// deviation
		System.out.println("DEVIATION: invalid action");
	}

	public void monitorWaitingTime(Long taskTime) {
		TaskMetaDataContext taskMeta = TaskMetaDataContext.of(Policy.of("admin", List.of("managers")));

		ExtendedDataContext tasks = this.taskSvc.get(this.pid.tasks(), taskMeta);
		List<TaskInstanceId> taskIdList = tasks.meta().as(TaskWorkItemDataContext.class).tasks();

		for (TaskInstanceId taskId: taskIdList) {
			System.out.println("task available: " + taskId.taskId().taskId());
			this.timeMonitor.monitorWaiting(taskId.taskId().taskId(), taskTime);
		}
	}

	public void setXMI(SetXMICommand modelData) throws IOException {
		this.setXMI(modelData.getModel(), modelData.getTimestamp());
	}

	public synchronized void setXMI(String modelXMI, Long timestamp) throws IOException {
		LocalProcessId id = appRoot.get(ProcessIds.class).get("process_1");
		MapDataContext ctx = mapper.readValue("{}", MapDataContext.class);

		this.process = processSvc.create(id, ctx);
		this.pid = this.process.meta().as(ProcessMetaDataContext.class).id(ProcessInstanceId.class);
		System.out.println(this.pid);

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

}
