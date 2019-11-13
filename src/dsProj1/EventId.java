package dsProj1;

//Support libraries
import org.eclipse.jdt.annotation.NonNull;

// Standard libraries
import java.util.UUID;

public class EventId {
	public final long id; // This is is incremental (can be so as the increment is local to the source)
	public final @NonNull UUID source;
	
	public EventId(@NonNull UUID source, long id) {
		this.id = id;
		this.source = source;
	}
	
	@Override
	public String toString() {
		return "EventId[source="+ this.source + ", id=" + this.id +"]";
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof EventId)) 
			return false;
		
		EventId other = (EventId) obj;

		return this.source.equals(other.source) && this.id == other.id;
	}
}
