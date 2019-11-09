package dsProj1;

//Support libraries
import org.eclipse.jdt.annotation.NonNull;

// Standard libraries
import java.util.UUID;

public class EventId {
	public final @NonNull UUID id;
	public final @NonNull UUID source;
	
	public EventId(@NonNull UUID source) {
		this.id = UUID.randomUUID();
		this.source = source;
	}
	
	@Override
	public String toString() {
		return "{ source: " + this.source + ", id: " + this.id + "}";
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof EventId)) 
			return false;
		
		EventId other = (EventId) obj;
		
		return this.source.equals(other.source) && this.id.equals(other.id);
	}
}
