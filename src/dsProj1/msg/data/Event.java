package dsProj1.msg.data;

// Support libraries
import org.eclipse.jdt.annotation.NonNull;

// Standard libraries
import java.util.UUID;

public class Event {
	public final @NonNull UUID eventId;
	
	public Event(@NonNull UUID eventId) {
		this.eventId = eventId;
	}
	
	@Override
	public String toString() {
		return "{ eventId: " + this.eventId + " }";
	}
}