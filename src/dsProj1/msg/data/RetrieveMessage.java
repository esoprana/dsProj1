package dsProj1.msg.data;

// Support libraries
import org.eclipse.jdt.annotation.NonNull;

// Custom libraries
import dsProj1.EventId;

public class RetrieveMessage {
	public final @NonNull EventId eventIdRequested;
	
	public RetrieveMessage(@NonNull EventId eId) {
		this.eventIdRequested = eId;
	}
	
	@Override
	public String toString() {
		return "RetriveMessage[eventIdRequested=" + this.eventIdRequested +"]";
	}
}
