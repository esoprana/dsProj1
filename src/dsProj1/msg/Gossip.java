package dsProj1.msg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import dsProj1.Options;

public class Gossip extends GenericMessage{
	public @NonNull List<@NonNull UUID> unSubs;   // Un-subscriptions
	public @NonNull List<@NonNull UUID> subs;     // Subscriptions
	public @NonNull List<@NonNull UUID> eventIds; // Ids of already handled events already handled
	public @NonNull List<@NonNull Event> events;  // Event ids
	
	public Gossip() {
		this.unSubs = new ArrayList<@NonNull UUID>(Options.UN_SUBS_SIZE);          // Un-subscriptions
		this.subs = new ArrayList<@NonNull UUID>(Options.SUBS_SIZE);               // Subscriptions
		this.eventIds = new ArrayList<@NonNull UUID>(Options.EVENT_IDS_SIZE);      // Events already handled
		this.events = new ArrayList<@NonNull Event>(Options.EVENTS_SIZE);	
	}
	
	public Gossip(@NonNull Collection<@NonNull UUID> subs, 
				  @NonNull Collection<@NonNull UUID> unSubs, 
				  @NonNull Collection<@NonNull UUID> eventIds, 
				  @NonNull Collection<@NonNull Event> events) {
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
		this.events = new ArrayList<@NonNull Event>(events);
	}
}
