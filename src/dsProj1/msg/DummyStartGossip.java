package dsProj1.msg;

import java.util.UUID;

public class DummyStartGossip extends GenericMessage {

	public DummyStartGossip(UUID id) {
		this.process_id = id;
		this.dest = id;
	}
}
