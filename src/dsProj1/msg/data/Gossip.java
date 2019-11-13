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
import dsProj1.EventId;
import dsProj1.msg.data.Event;


public class Gossip {
	public final @NonNull List<@NonNull UUID> unSubs;          // Un-subscriptions
	public final @NonNull List<@NonNull UUID> subs;            // Subscriptions
	public final @NonNull List<@NonNull EventId> eventIds; 	   // Ids of already handled events already handled
	public final @NonNull List<@NonNull Event> events; // Event ids
	
	public Gossip(@NonNull Collection<@NonNull UUID> subs, 
			  	  @NonNull Collection<@NonNull UUID> unSubs, 
			  	  @NonNull Collection<@NonNull EventId> eventIds, 
			  	  @NonNull Collection<@NonNull Event> events) {
//		if (subs.size() > Options.SUBS_SIZE) {
//			throw new IllegalArgumentException("The passed subs parameter should have at max " + Options.SUBS_SIZE + "elements");
//		}
//	
//		if (unSubs.size() > Options.UN_SUBS_SIZE) {
//			throw new IllegalArgumentException("The passed unSubs parameter should have at max " + Options.UN_SUBS_SIZE + "elements");
//		}
//	
//		if (eventIds.size() > Options.EVENT_IDS_SIZE) {
//			throw new IllegalArgumentException("The passed eventIds parameter should have at max " + Options.EVENT_IDS_SIZE + "elements");
//		}
//	
//		if (events.size() > Options.EVENTS_SIZE) {
//			throw new IllegalArgumentException("The passed events parameter should have at max " + Options.EVENTS_SIZE + "elements");
//		}
	
		this.unSubs = new ArrayList<@NonNull UUID>(unSubs);
		this.subs = new ArrayList<@NonNull UUID>(subs);
		this.eventIds = new ArrayList<@NonNull EventId>(eventIds);
		this.events = new ArrayList<@NonNull Event>(events);
	}
	
	@Override
	public String toString() {
		return "[subs=" + this.subs + ", unSubs=" + this.unSubs + ", eventIds=" + this.eventIds + ", events=" + this.events + "]";
	}
}
