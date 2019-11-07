package dsProj1;

import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

public class ToRetrieveEv {
	@NonNull
	public UUID event_id;
	
	@NonNull
	public UUID sender;

	// TODO: Use source
	//@NonNull
	//public UUID source;
	
	public long round;
	
	public long noRequests = 0;
	public long requestedAtRound = -1;
	
	public ToRetrieveEv(@NonNull UUID event_id, long round, @NonNull UUID sender) {
		this.event_id = event_id;
		this.round = round;
		this.sender = sender;
	}
}
