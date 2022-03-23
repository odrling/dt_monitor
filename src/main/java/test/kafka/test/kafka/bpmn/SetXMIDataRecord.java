package test.kafka.test.kafka.bpmn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class SetXMIDataRecord implements DataRecord {

	final static ObjectMapper mapper = new ObjectMapper();

	private String payload;

	public SetXMIDataRecord(String payload) {
		this.payload = payload;
	}

	@Override
	public ObjectNode toJson() {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode obj = mapper.createObjectNode();

		obj.put("command", Commands.SET_XMI.name());
		obj.put("payload", this.payload);
		return obj;
	}
}
