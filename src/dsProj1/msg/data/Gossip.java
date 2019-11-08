package dsProj1.msg.data;

// Support libraries
import org.eclipse.jdt.annotation.NonNull;

// Standard libraries
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

// Custom libraries
import dsProj1.Options; // Get runtime options


public class Gossip {
	public final @NonNull List<@NonNull UUID> unSubs;   // Un-subscriptions
	public final @NonNull List<@NonNull UUID> subs;     // Subscriptions
	public final @NonNull List<@NonNull UUID> eventIds; // Ids of already handled events already handled
	public final @NonNull List<dsProj1.msg.data.Event> events;  // Event ids
	
	public Gossip(@NonNull Collection<@NonNull UUID> subs, 
			  	  @NonNull Collection<@NonNull UUID> unSubs, 
			  	  @NonNull Collection<@NonNull UUID> eventIds, 
			  	  @NonNull Collection<dsProj1.msg.data.Event> events) {
		if (subs.size() > Options.SUBS_SIZE) {
			throw new IllegalArgumentException("The passed subs parameter should have at max " + Options.SUBS_SIZE + "elements");
		}
	
		if (unSubs.size() > Options.UN_SUBS_SIZE) {
			throw new IllegalArgumentException("The passed unSubs parameter should have at max " + Options.UN_SUBS_SIZE + "elements");
		}
	
		if (eventIds.size() > Options.EVENT_IDS_SIZE) {
			throw new IllegalArgumentException("The passed eventIds parameter should have at max " + Options.EVENT_IDS_SIZE + "elements");
		}
	
		if (events.size() > Options.EVENTS_SIZE) {
			throw new IllegalArgumentException("The passed events parameter should have at max " + Options.EVENTS_SIZE + "elements");
		}
	
		this.unSubs = new ArrayList<@NonNull UUID>(unSubs);
		this.subs = new ArrayList<@NonNull UUID>(subs);
		this.eventIds = new ArrayList<@NonNull UUID>(eventIds);
		this.events = new ArrayList<dsProj1.msg.data.Event>(events);
	}
	
	@Override
	public String toString() {
		return "{ subs: " + this.subs + ",\n" +
			   "  unSubs: " + this.unSubs + ",\n" +
			   "  eventIds: " + this.eventIds + ",\n" +
			   "  events: " + this.events + " }";
	}
}
