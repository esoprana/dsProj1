package dsProj1.msg;

import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

public class DummyStartGossip extends GenericMessage {
	public DummyStartGossip(@NonNull UUID id) {
		this.process_id = id;
		this.dest = id;
	}
}
