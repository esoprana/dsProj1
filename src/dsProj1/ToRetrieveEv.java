package dsProj1;

//Support libraries
import org.eclipse.jdt.annotation.NonNull;

// Standard libraries
import java.util.UUID;

public class ToRetrieveEv {
	
	public final @NonNull EventId eventId;
	public final @NonNull UUID sender;
	
	public long round;
	
	public long noRequests = 0;
	public long requestedAtRound = -1;
	
	public ToRetrieveEv(@NonNull EventId eventId, long round, @NonNull UUID sender) {
		this.eventId = eventId;
		this.round = round;
		this.sender = sender;
	}
}
