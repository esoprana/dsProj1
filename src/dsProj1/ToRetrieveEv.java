package dsProj1;

//Support libraries
import org.eclipse.jdt.annotation.NonNull;

// Standard libraries
import java.util.UUID;

public class ToRetrieveEv {
	
	public final @NonNull EventId eventId;
	public final @NonNull UUID sender;
	
	public long round;
	
	public int noRequests = 0;
	public long requestedAtRound = -1;
	
	public ToRetrieveEv(@NonNull EventId eventId, long round, @NonNull UUID sender) {
		this.eventId = eventId;
		this.round = round;
		this.sender = sender;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ToRetrieveEv)) {
			return false;
		}
		
		ToRetrieveEv tev = (ToRetrieveEv) obj;
		
		return this.eventId.equals(tev.eventId);
	}
	
	@Override
	public String toString() {
		return "ToRetrieveEv[eventId=" + this.eventId + ", sender=" + this.sender + "]"; // TODO: Maybe add other stuff
	}
}
