package dsProj1;

import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

public class ToRetrieveEv {
	
	public final @NonNull UUID eventId;
	public final @NonNull UUID sender;

	// TODO: Use source
	//@NonNull
	//public UUID source;
	
	public long round;
	
	public long noRequests = 0;
	public long requestedAtRound = -1;
	
	public ToRetrieveEv(@NonNull UUID eventId, long round, @NonNull UUID sender) {
		this.eventId = eventId;
		this.round = round;
		this.sender = sender;
	}
}
