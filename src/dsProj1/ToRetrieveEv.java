package dsProj1;

import java.util.UUID;

public class ToRetrieveEv {
	public UUID event_id;
	public long round;
	public UUID sender;
	public UUID source;
	
	public long noRequests = 0;
	public long requestedAtRound = -1;
	
	public ToRetrieveEv(UUID event_id, long round, UUID sender) {
		this.event_id = event_id;
		this.round = round;
		this.sender = sender;
	}
}
