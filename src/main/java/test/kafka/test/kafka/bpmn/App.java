package test.kafka.test.kafka.bpmn;

import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.DocumentRoot;
import org.eclipse.bpmn2.util.Bpmn2ResourceFactoryImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceFactoryImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

public class App {
	public static void main(String[] args) {
		ResourceFactoryImpl bpmnFactory = new Bpmn2ResourceFactoryImpl();

		System.out.println(Bpmn2Package.eNS_URI);
		if (!EPackage.Registry.INSTANCE.containsKey(Bpmn2Package.eNS_URI)) {
			EPackage.Registry.INSTANCE.put(Bpmn2Package.eNS_URI, Bpmn2Package.eINSTANCE);
		}
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("bpmn", bpmnFactory);
		ResourceSet rs = new ResourceSetImpl();
		URI inUri = URI.createURI("file:/home/odrling/eclipse-workspaces/gemoc-xbpmn/test.bpmn/examples/process_1.bpmn");
		Resource resource = rs.getResource(inUri, true);

		DocumentRoot root = (DocumentRoot) resource.getContents().get(0);
		System.out.println(root.eContents());
	}
}
