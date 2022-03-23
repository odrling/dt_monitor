package test.kafka.test.kafka.bpmn;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

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

import test.kafka.test.kafka.bpmn.avro.Command;
import test.kafka.test.kafka.bpmn.avro.ElementEvent;
import test.kafka.test.kafka.bpmn.avro.SetXMICommand;
import test.kafka.test.kafka.bpmn.avro.event;
import test.kafka.test.kafka.bpmn.statemachine.Action;
import test.kafka.test.kafka.bpmn.statemachine.Deviation;
import test.kafka.test.kafka.bpmn.statemachine.StateMachine;
import test.kafka.test.kafka.bpmn.statemachine.Status;

@Singleton
public class Model {

	private DocumentRoot root;
	private StateMachine stateMachine;

	public DocumentRoot getRoot() {
		return root;
	}

	public void setRoot(DocumentRoot root) {
		this.root = root;
	}

	public Model() {
		this.root = null;
	}

	private interface Handler {

		void handle(Model model, Object commandData);

	}

	private static final Map<Class<? extends Object>, Handler> dispatch = new HashMap<>();
	static {
		dispatch.put(SetXMICommand.class, new Handler() {
			@Override
			public void handle(Model model, Object cmdData) {
				SetXMICommand commandData = (SetXMICommand) cmdData;
				try {
					model.setXMI(commandData.getSetXmi());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		dispatch.put(ElementEvent.class, new Handler() {
			@Override
			public void handle(Model model, Object cmdData) {
				ElementEvent commandData = (ElementEvent) cmdData;

				FlowNode node;
				try {
					node = (FlowNode) model.findID(commandData.getElementID());
				} catch (EObjectNotFound e) {
					System.out.println("Could not find " + commandData.getElementID());
					e.printStackTrace();
					return;
				}

				Action act;
				if (commandData.getEvent() == event.Start) {
					act = Action.get(node, Status.ACTIVE);
				} else if (commandData.getEvent() == event.End) {
					act = Action.get(node, Status.COMPLETED);
				} else {
					throw new RuntimeException("invalid event status");
				}

				try {
					model.stateMachine.applyAction(act);
				} catch (Deviation e) {
					System.out.println(e.getRelatedNodes());
				}
			}
		});
	}

	public void handle(Command cmd) {
		Object commandData = cmd.getCommand();
		System.out.println("handling " + commandData.getClass().getName());
		Handler handler = dispatch.get(commandData.getClass());
		handler.handle(this, commandData);
		System.out.println("done handling " + commandData.getClass().getName());
	}

	public void setXMI(String modelXMI) throws IOException {
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
		this.stateMachine = StateMachine.fromBPMNRoot(this.root);
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
