package dsProj1;

// Support libraries
import org.eclipse.jdt.annotation.NonNull;

// Standard libraries
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

// Custom libraries
import dsProj1.msg.Message;
import dsProj1.msg.data.RoundStart;
import dsProj1.msg.data.Event;
import dsProj1.msg.data.ExternalData;
import dsProj1.msg.data.Gossip;
import dsProj1.msg.data.RetrieveMessage;

public class Node {
	private final @NonNull Oracle oracle;

	public Node(@NonNull UUID id, 
				@NonNull Collection<@NonNull UUID> initialView, 
				@NonNull Oracle oracle) {
		if (initialView.size() > Options.VIEWS_SIZE) {
			throw new IllegalArgumentException("initialView parameter can't be larger than " + Options.VIEWS_SIZE);
		}

		this.id = id;
		this.view.addAll(initialView);

		this.oracle = oracle;
	}
	
	// TODO: Fare diagrammi articolo + magari qualcosina
	
	@NonNull List<@NonNull UUID> view = new ArrayList<@NonNull UUID>(2*Options.VIEWS_SIZE);              					   // Individual view // TODO: Fix permission(package -> private)
	private @NonNull List<@NonNull UUID> unSubs = new ArrayList<@NonNull UUID>(2*Options.UN_SUBS_SIZE);          			   // Un-subscriptions
	private @NonNull List<@NonNull UUID> subs = new ArrayList<@NonNull UUID>(2*Options.SUBS_SIZE);               			   // Subscriptions
	private @NonNull List<@NonNull EventId> eventIds	= new ArrayList<@NonNull EventId>(2*Options.EVENT_IDS_SIZE);      	   // Events already handled
	private @NonNull List<@NonNull ToRetrieveEv> retrieveBuf = new ArrayList<@NonNull ToRetrieveEv>(2*Options.RETRIEVE_SIZE);  // Events to be retrieved

	private @NonNull List<@NonNull Event> events = new ArrayList<@NonNull Event>(2*Options.EVENTS_SIZE);

	public @NonNull UUID id;
	private long nextEventId = 0;

	public boolean alive = true;
	private long currentRound = 1;
	
	public static void shuffleTrim(@NonNull List<?> l, int dim) {
		if (l.size() <= dim) 
			return;
		
		Collections.shuffle(l);
		l.subList(dim, l.size()).clear();			
	}
	
	private void trimEventIds() {
		if (this.eventIds.size() <= Options.EVENT_IDS_SIZE) 
			return;

		Map<UUID, ArrayList<EventId>> sourceToIds = this.eventIds.stream().collect(Collectors.groupingBy(
				(EventId e) -> e.source, Collectors.toCollection(ArrayList::new)
		));

		ArrayList<List<EventId>> groupedList = sourceToIds.values().stream().map((ArrayList<EventId> evs) -> {
			evs.sort(Comparator.comparing((EventId e) -> -e.id));
			long lim = evs.get(0).id - Options.LONG_AGO;
			int b = 0;
			for (;b<evs.size(); ++b) {
				if (evs.get(b).id < lim) {
					break;
				}
			}
			
			return evs.subList(b, evs.size());
		}).collect(Collectors.toCollection(ArrayList::new));

		int l = this.eventIds.size() - Options.EVENT_IDS_SIZE;
		for (int i = 0; l>0 && !groupedList.isEmpty();) { // Remove uniformly // TODO: Check if this policy is fine
			List<EventId> k = groupedList.get(i % groupedList.size());
			
			if (k.size() == 0) {
				groupedList.remove(i % groupedList.size());
			} else {
				k.remove(k.size()-1);
				--l;				
				++i;
			}
		}
		
		this.eventIds.clear();
		sourceToIds.values().stream().flatMap(k -> k.stream()).forEach(this.eventIds::add);
		
		// If more elements remain delete
		shuffleTrim(this.eventIds, Options.EVENT_IDS_SIZE);
	}
	
	private void trimEvents() {
		if (this.events.size() <= Options.EVENTS_SIZE) 
			return;

		Map<UUID, ArrayList<Event>> sourceToIds = this.events.stream().collect(Collectors.groupingBy(
				(Event e) -> e.eventId.source, Collectors.toCollection(ArrayList::new)
		));
		
		ArrayList<List<Event>> groupedList = sourceToIds.values().stream().map((ArrayList<Event> evs) -> {
			evs.sort(Comparator.comparing((Event e) -> -e.eventId.id));
			long lim = evs.get(0).eventId.id - Options.LONG_AGO;
			int b = 0;
			for (;b<evs.size(); ++b) {
				if (lim > evs.get(b).eventId.id) {
					break;
				}
			}
			
			return evs.subList(b, evs.size());
		}).collect(Collectors.toCollection(ArrayList::new));
		

		int l = this.events.size() - Options.EVENTS_SIZE;
		for (int i = 0; l>0 && !groupedList.isEmpty();) { // Remove uniformly // TODO: Check if this policy is fine
			List<Event> k = groupedList.get(i % groupedList.size());
			
			if (k.size() == 0) {
				groupedList.remove(i % groupedList.size());
			} else {
				k.remove(k.size()-1);
				--l;				
				++i;
			}
		}
		
		// Now that we removed the out-of-date items we can just get the first Options.EVENTS_SIZE with the smallest age
		this.events.clear();
		sourceToIds.values().stream()
				   			.flatMap(k -> k.stream())
				   			.sorted(Comparator.comparing( (Event e) -> e.age))
				   			.limit(Options.EVENTS_SIZE)
				   			.forEach(this.events::add);
		
		// THIS IS BEFORE OPTIMIZATION
		// If there are still too many events remove randomly
		// shuffleTrim(this.events, Options.EVENTS_SIZE);
	}
	
	public void handleMessage(Message msg) throws Exception {
		if (msg.data instanceof RoundStart) {
			this.startRound();
		} else if (msg.data instanceof ExternalData) {
			this.lpbCast(new Event<>(this.id, this.nextEventId++, ((ExternalData) msg.data).data));
		} else if (msg.data instanceof Gossip) {
			this.handleGossip(msg);
		} else if (msg.data instanceof Event) {
			this.handleEvent((Event)msg.data);
		} else if (msg.data instanceof RetrieveMessage) {
			this.handleRetrieve(msg);
		} else {
			throw new Exception("Unrecognized type of message!");
		}
	}
	
	private void handleGossip(@NonNull Message<Gossip> g) {
		// PHASE 1: UPDATE VIEW AND UNSUBS
		this.view.removeAll(g.data.unSubs); // Remove all gossip unsubs from global view
		this.subs.removeAll(g.data.unSubs); // Remove all gossip unsubs from global subs
		
		g.data.unSubs.stream()
				     .filter((UUID it) -> !this.unSubs.contains(it)) // Remove already contained
				     .forEach(this.unSubs::add);     				 // Add unsubs from gossip to local unsubs
				
		// Randomly select UN_SUBS_SIZE values from unsub, this is the new unsubs		
		shuffleTrim(this.unSubs, Options.UN_SUBS_SIZE);
		
		// PHASE 2: ADD NEW SUBSCRIPTIONS
		g.data.subs.stream() // TODO: Check variables
			  	   .filter((UUID it) -> !this.view.contains(it) && !it.equals(this.id))
			  	   .forEach((UUID it) -> {
			  		   if (!this.subs.contains(it)) this.subs.add(it);
					 
					   this.view.add(it);
			  	   });
		
		if (this.view.size() > Options.VIEWS_SIZE) {
			Collections.shuffle(this.view);
			List<UUID> t = this.view.subList(Options.VIEWS_SIZE, this.view.size());
			this.subs.removeAll(t);
			t.clear();
		}
		
		shuffleTrim(this.subs, Options.SUBS_SIZE);
		
		// PHASE 3: UPDATE EVENTS WITH NEW NOTIFICATIONS
		g.data.events
			  .forEach(this::handleEvent); // Handle all new events
		
		g.data.eventIds
		 	  .stream()
		 	  .filter( (EventId it) -> !this.eventIds.contains(it) )
		 	  .map( (EventId it) -> new ToRetrieveEv(it, this.currentRound, g.source))
			  .forEach( k -> {
				  if (this.retrieveBuf.contains(k))
					  return;
				  
				  this.retrieveBuf.add(k);
			  });

		// Fix size of EVENT_IDS
		this.trimEventIds();
		this.trimEvents();
	}

	private void requestRetrieve(@NonNull EventId eventId, @NonNull UUID dest) {
		RetrieveMessage rm = new RetrieveMessage(eventId);
		System.out.println(rm);
		this.send(dest, rm);
	}

	private void send(@NonNull UUID destination, @NonNull Object data) {
		this.oracle.send(new Message<>(this.id, destination, data));
	}

	private void scheduleGossip(double delay) {
		this.oracle.scheduleGossip(delay, new Message<RoundStart>(this.id, this.id, new RoundStart()));
	}
	
	private void lpbCast(@NonNull Event e) { // TODO: To integrate in some way with sending
		// If the event was already (completely/in full) received ignore
		if (this.events.contains(e))
			return;

		// Otherwise, add
		this.events.add(e);
	}

	private void handleEvent(@NonNull Event ev) {
		// If the event was already (completely) received ignore
		if (this.eventIds.contains(ev.eventId))
			return;

		this.events.add(ev);

		// Deliver the event to the application
		this.lpbDeliver(ev);

		// Add the id of the event
		this.eventIds.add(ev.eventId);
	}
	
	private void handleRetrieve(Message<RetrieveMessage> rm) throws Exception {
		Iterator<Event> it = this.events.iterator();
		while (it.hasNext()) {
			Event e = it.next();
			
			if (e.eventId.equals(rm.data.eventIdRequested)) {
				this.send(rm.source, e);
				return;
			}
		}

		return; // TODO: Should I respond instead? (I think so)
	}

	private void lpbDeliver(@NonNull Event ev) {
		//System.out.println("Node[" + this.id + "] received Event[" + ev.data + "]");
	}

	private void emitGossip() {
		Random rand = new Random();
			
		// Select FANOUT_SIZE targets to which send the gossip
		List<@NonNull UUID> targets = new ArrayList<>(Options.FANOUT_SIZE);
		while (targets.size() < Options.FANOUT_SIZE) {
			UUID w = this.view.get(rand.nextInt(this.view.size()));
			
			if (!targets.contains(w)) {
				targets.add(w);
			}
		}

		// Add ourselves to the subs list (if there is enough space just add, otherwise substitute at random)
		List<@NonNull UUID> s = new ArrayList<>(this.subs);
		if (s.size() < Options.SUBS_SIZE)
			s.add(this.id);
		else 
			s.set(rand.nextInt(this.subs.size()), this.id);
		
		// Create the gossip and sent it
		Gossip g = new Gossip(s, this.unSubs, this.eventIds, this.events);
		targets.forEach(t -> this.send(t, g));
				 
		this.events.clear();

		// Schedule another gossip in Options.GOSSIP_INTERVAL seconds
		this.scheduleGossip(Options.GOSSIP_INTERVAL);
	}

	private void retrieve() {
		Iterator<ToRetrieveEv> it = this.retrieveBuf.iterator();
		
		while (it.hasNext()) {
			ToRetrieveEv el = it.next();
			
			if (this.currentRound - el.round <= Options.OLD_TIME_RETRIEVE) {
				// If it's still too early, don't worry
			} else if (this.eventIds.contains(el.eventId)) {
				// If we already received it, remove it
				it.remove();
			} else {
				switch(el.noRequests) {
					case -1: // If is the first time this happens ask to who sent us the related gossip
						this.requestRetrieve(el.eventId, el.sender);
						el.noRequests++;
						el.requestedAtRound = this.currentRound;
						break;
					case 0: // If it's the second time ask to a random node
						if (this.currentRound > el.requestedAtRound + Options.REQUEST_TIMEOUT_ROUNDS ) {
							this.requestRetrieve(el.eventId, this.view.get(new Random().nextInt(this.view.size())));
							el.noRequests++;
							el.requestedAtRound = this.currentRound;
						}
					case 1: // If it's the third time ask source
						if (this.currentRound > el.requestedAtRound + Options.REQUEST_TIMEOUT_ROUNDS ) {
							this.requestRetrieve(el.eventId, el.eventId.source); // How to get the source?
							el.noRequests++;
							el.requestedAtRound = this.currentRound;
						}
						break;
					case 2: // If 3° time failed just fail
						if (this.currentRound > el.requestedAtRound + Options.REQUEST_TIMEOUT_ROUNDS ) {
							// Packet is considered lost, log something
						}
						break;
				}
			}
		}
	}
	
	public void startRound() {
		++this.currentRound;
		this.events.forEach(e -> ++e.age);
		this.retrieve();
		this.emitGossip();
	}
}
