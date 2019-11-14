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
import java.util.Optional;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// Custom libraries
import dsProj1.msg.Message;
import dsProj1.msg.data.RoundStart;
import repast.simphony.random.RandomHelper;
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
	
	private boolean alreadySeenEventId(EventId eId) {
		if (this.eventIds.contains(eId)) {
			return true;
		}

		Optional<EventId> min = this.eventIds.stream()
					 						 .filter( id -> id.source == eId.source)
					 						 .min( Comparator.comparing(id -> id.id) );

		return min.isPresent() && eId.id < min.get().id;
	}
	
	private void addEventId(EventId eId) {
		// Find last id received in sequence from eId.soruce
		Optional<EventId> min = this.eventIds.stream()
				 							 .filter( id -> id.source == eId.source)
				 							 .min( Comparator.comparing(id -> id.id) );

		if (!min.isPresent() || min.get().id != eId.id-1) {
			this.eventIds.add(eId);
			return;
		}
		
		final long newId;
		
		{
			long toSearchId = eId.id+1;
			for(;this.eventIds.contains(new EventId(eId.source, toSearchId));++toSearchId);

			newId = toSearchId - 1;
		}
		
		this.eventIds.removeIf(id -> eId.source.equals(id.source) && id.id <= newId);
		this.eventIds.add(new EventId(eId.source, newId)); // -1 as newId contains the last one which was not found
	}
	
	private void trimEventIds() {
		if (this.eventIds.size() <= Options.EVENT_IDS_SIZE) 
			return;

		Map<UUID, ArrayList<EventId>> sourceToIds = this.eventIds.stream().collect(Collectors.groupingBy(
				(EventId e) -> e.source, Collectors.toCollection(ArrayList::new)
		));

		ArrayList<List<EventId>> groupedList = sourceToIds.values().stream().map((ArrayList<EventId> evs) -> {
			evs.sort(Comparator.comparing((EventId e) -> -e.id));
			return evs;
		}).collect(Collectors.toCollection(ArrayList::new));
		
		int l = this.eventIds.size() - Options.EVENT_IDS_SIZE;
		for (; l>0;) { // Remove uniformly // TODO: Check if this policy is fine
			Optional<List<EventId>> k = groupedList.stream()
										.map(w -> {
											int b = 0;
											for (;b<w.size() && w.get(0).id - w.get(b).id > Options.LONG_AGO;++b);
											
											return w.subList(b, w.size());
										})
									   .filter(w -> w.size() > 0)
					   				   .max(Comparator.comparing(w -> w.size()));
			
			// No remaining sublist from which to remove (groupList is empty or all list have w.get(0).eventId.id - w.get(w.size()-1).eventId.id > Options.LONG_AGO)
			if (!k.isPresent()) 
				break;
			
			List<EventId> evs = k.get();
			evs.remove(evs.size()-1);
			--l;
			
			while (l > 0 && evs.size() > 1 && evs.get(evs.size()-2).id+1 == evs.get(evs.size()-1).id) {
				evs.remove(evs.size()-1);
				--l;
			}
		}

		/*
		int l = this.eventIds.size() - Options.EVENT_IDS_SIZE;
		for (int i = 0; l>0 && !groupedList.isEmpty();) { // Remove uniformly // TODO: Check if this policy is fine
			List<EventId> k = groupedList.get(i % groupedList.size());
			
			if (k.size() == 0 || k.get(0).id - k.get(k.size()-1).id > Options.LONG_AGO) {
				groupedList.remove(i % groupedList.size());
			} else {
				k.remove(k.size()-1);
				--l;
				
				for (int w=1; w<k.size()-1 && 
							  k.get(0).id - k.get(k.size()-1).id > Options.LONG_AGO && 
							  k.get(k.size()-w).id +1 == k.get(k.size()-w-1).id ; w++) {
					k.remove(w);
				}
				
				++i;
			}
		}
		*/
		
		this.eventIds.clear();
		sourceToIds.values().stream().flatMap(k -> k.stream()).forEach(this::addEventId);
		
		// If more elements remain delete
		shuffleTrim(this.eventIds, Options.EVENT_IDS_SIZE);
		
		// Once some were deleted randomly, ensure that last element is cumulative holds
		ArrayList<EventId> eIds = new ArrayList<EventId>(this.eventIds);
		this.eventIds.clear();
		eIds.forEach(this::addEventId);
	}
	
	private void trimEvents() {
		if (this.events.size() <= Options.EVENTS_SIZE) 
			return;

		Map<UUID, ArrayList<Event>> sourceToIds = this.events.stream().collect(Collectors.groupingBy(
				(Event e) -> e.eventId.source, Collectors.toCollection(ArrayList::new)
		));
		
		ArrayList<List<Event>> groupedList = sourceToIds.values().stream().map((ArrayList<Event> evs) -> {
			evs.sort(Comparator.comparing((Event e) -> -e.eventId.id));
			return evs;
		}).collect(Collectors.toCollection(ArrayList::new));
		
		/*
		int l = this.events.size() - Options.EVENTS_SIZE;
		for (int i = 0; l>0 && !groupedList.isEmpty();) { // Remove uniformly // TODO: Check if this policy is fine
			List<Event> k = groupedList.get(i % groupedList.size());
			
			if (k.size() == 0 || k.get(0).eventId.id - k.get(k.size()-1).eventId.id > Options.LONG_AGO) {
				groupedList.remove(i % groupedList.size());
			} else {
				k.remove(k.size()-1);
				--l;
				
				while (l > 0 && k.size() > 1 && k.get(0).eventId.id - k.get(k.size()-1).eventId.id > Options.LONG_AGO && k.get(k.size()-2).eventId.id+1 == k.get(k.size()-1).eventId.id) {
					k.remove(k.size()-1);
					--l;
				}

				++i;
			}
		}
		*/
		
		int l = this.events.size() - Options.EVENTS_SIZE;
		for (; l>0;) { // Remove uniformly // TODO: Check if this policy is fine
			Optional<List<Event>> k = groupedList.stream()
										.map(w -> {
											int b = 0;
											for (;b<w.size() && w.get(0).eventId.id - w.get(b).eventId.id > Options.LONG_AGO;++b);
											
											return w.subList(b, w.size());
										})
									   .filter(w -> w.size() > 0)
					   				   .max(Comparator.comparing(w -> w.size()));
			
			// No remaining sublist from which to remove (groupList is empty or all list have w.get(0).eventId.id - w.get(w.size()-1).eventId.id > Options.LONG_AGO)
			if (!k.isPresent()) 
				break;
			
			List<Event> evs = k.get();
			evs.remove(evs.size()-1);
			--l;
			
			while (l > 0 && evs.size() > 1 && evs.get(evs.size()-2).eventId.id+1 == evs.get(evs.size()-1).eventId.id) {
				evs.remove(evs.size()-1);
				--l;
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
		 	  .filter( (EventId it) -> !this.alreadySeenEventId(it))
		 	  .map( (EventId it) -> new ToRetrieveEv(it, this.currentRound, g.source))
			  .forEach( k -> {
				  if (this.retrieveBuf.contains(k))
					  return;
				  
				  this.retrieveBuf.add(k);
			  });
				
//		Map<UUID, Optional<@NonNull EventId>> evs = this.eventIds.stream()
//					 											 .collect(
//					 													Collectors.groupingBy( (EventId e) -> e.source,		 
//					 													Collectors.minBy( Comparator.comparing((EventId e) -> e.id) )));
//		
//		g.data.eventIds.stream()
//					   .collect(Collectors.groupingBy((EventId e) -> e.source, Collectors.minBy( Comparator.comparing((EventId e) -> e.id)  )))
//					   .values()
//					   .stream()
//					   .forEach( (Optional<EventId> oEv) -> {
//						   EventId ev = oEv.get();
//						   
//						   if (evs.containsKey(ev.source)) {
//							   EventId toComp = evs.get(ev.source).get();
//							   
//							   for(long i = ev.id-1; i > toComp.id; --i) {
//								   ToRetrieveEv toRetrieveEv = new ToRetrieveEv(new EventId(ev.source, i), this.currentRound, g.source);
//								   
//								   if (!this.retrieveBuf.contains(toRetrieveEv)) {
//									   this.retrieveBuf.add(toRetrieveEv);								   
//								   }
//							   }
//						   }
//					   });

		// Fix size of EVENT_IDS
		this.trimEventIds();
		this.trimEvents();
	}

	private void requestRetrieve(@NonNull EventId eventId, @NonNull UUID dest) {
		RetrieveMessage rm = new RetrieveMessage(eventId);
		this.send(dest, rm);
	}

	private void send(@NonNull UUID destination, @NonNull Object data) {
		this.oracle.send(new Message<>(this.id, destination, data));
	}

	private void scheduleGossip(double delay) {
		this.oracle.scheduleGossip(delay, new Message<RoundStart>(this.id, this.id, new RoundStart()));
	}
	
	public void lpbCast(@NonNull Event e) { // TODO: To integrate in some way with sending
		// If the event was already (completely/in full) received ignore
		if (this.events.contains(e))
			return;

		// Otherwise, add
		this.events.add(e);
	}

	private void handleEvent(@NonNull Event ev) {
		// If the event was already (completely) received ignore
		if (this.alreadySeenEventId(ev.eventId))
			return;

		this.events.add(ev);

		// Deliver the event to the application
		this.lpbDeliver(ev);

		// Add the id of the event
		this.addEventId(ev.eventId);
		
		Iterator<ToRetrieveEv> it = this.retrieveBuf.iterator();
		while(it.hasNext()) {
			ToRetrieveEv tre = it.next();
			if (tre.eventId.equals(ev.eventId)) {
				it.remove();
				return;
			}
		}
	}
	
	private void handleRetrieve(Message<RetrieveMessage> rm) throws Exception {
		Iterator<Event> it = this.events.iterator();
		while (it.hasNext()) {
			Event e = it.next();
			
			if (e.eventId.equals(rm.data.eventIdRequested)) {
				System.err.println("FOUND!!!");
				this.send(rm.source, e);
				return;
			}
		}

		return; // TODO: Should I respond instead? (I think so)
	}

	private void lpbDeliver(@NonNull Event ev) {
		this.oracle.stat(this.id, ev);
		//System.out.println("Node[" + this.id + "] received Event[" + ev.data + "]");
	}

	private void emitGossip() {
		// Select FANOUT_SIZE targets to which send the gossip
		List<@NonNull UUID> targets = new ArrayList<>(Options.FANOUT_SIZE);
		while (targets.size() < Options.FANOUT_SIZE) {
			
			UUID w = this.view.get(RandomHelper.nextIntFromTo(0, this.view.size()-1));
			
			if (!targets.contains(w)) {
				targets.add(w);
			}
		}

		// Add ourselves to the subs list (if there is enough space just add, otherwise substitute at random)
		List<@NonNull UUID> s = new ArrayList<>(this.subs);
		if (s.size() < Options.SUBS_SIZE)
			s.add(this.id);
		else 
			s.set(RandomHelper.nextIntFromTo(0, this.view.size()-1), this.id);
		
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
							this.requestRetrieve(el.eventId, this.view.get( RandomHelper.nextIntFromTo(0, this.view.size()-1) ));
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
							System.err.println("FAILURE!");
						}
						break;
				}
			}
		}
	}
	
	public void startRound() {
		++this.currentRound;
		this.events.forEach(e -> ++e.age);
		this.emitGossip();
		this.retrieve();
	}
}
