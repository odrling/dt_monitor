package test.kafka.test.kafka.bpmn;

public class EObjectNotFound extends Exception {

	private String id;

	public EObjectNotFound(String id) {
		this.setId(id);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}
