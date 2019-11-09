package dsProj1.msg.data;

// Support libraries
import org.eclipse.jdt.annotation.NonNull;

// Standard libraries
import java.util.UUID;

// Custom libraries
import dsProj1.EventId;

public class Event<T> {
	public final @NonNull EventId eventId;
	public final @NonNull T data;
	
	public Event(@NonNull UUID source, @NonNull T data) {
		this.eventId = new EventId(source);
		this.data = data;
	}
	
	@Override
	public String toString() {
		return "{ eventId: " + this.eventId + " }";
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Event))
			return false;
		
		Event other = (Event) obj;
		
		return this.eventId.equals(other.eventId);
	}
}