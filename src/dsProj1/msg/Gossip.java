package dsProj1.msg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import dsProj1.Options;

public class Gossip extends GenericMessage{
	public List<UUID> unSubs = new ArrayList<UUID>(Options.UN_SUBS_SIZE);          // Un-subscriptions
	public List<UUID> subs = new ArrayList<UUID>(Options.SUBS_SIZE);               // Subscriptions
	public List<UUID> eventIds	= new ArrayList<UUID>(Options.EVENT_IDS_SIZE);       // Events already handled
	public List<Event> events = new ArrayList<Event>(Options.EVENTS_SIZE);
	
	public Gossip() {
		this.unSubs = new ArrayList<UUID>(Options.UN_SUBS_SIZE);          // Un-subscriptions
		this.subs = new ArrayList<UUID>(Options.SUBS_SIZE);               // Subscriptions
		this.eventIds	= new ArrayList<UUID>(Options.EVENT_IDS_SIZE);       // Events already handled
		this.events = new ArrayList<Event>(Options.EVENTS_SIZE);	
	}
	
	public Gossip(Collection<UUID> subs, 
				  Collection<UUID> unSubs, 
				  Collection<UUID> eventIds, 
				  Collection<Event> events) {
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

		this.unSubs = new ArrayList<UUID>(unSubs);
		this.subs = new ArrayList<UUID>(subs);
		this.eventIds = new ArrayList<UUID>(eventIds);
		this.events = new ArrayList<Event>(events);
	}
}
