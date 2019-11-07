package dsProj1.msg;

import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

public class Event extends GenericMessage {
	@NonNull
	public final UUID id;
	
	public Event(@NonNull UUID eventId) {
		this.id = eventId;
	}
}
