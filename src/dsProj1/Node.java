package dsProj1;

// Support libraries
import org.eclipse.jdt.annotation.NonNull;

// Standard libraries
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

// Custom libraries
import dsProj1.msg.Message;
import dsProj1.msg.data.RoundStart;
import repast.simphony.random.RandomHelper;
import dsProj1.msg.data.Event;
import dsProj1.msg.data.ExternalData;
import dsProj1.msg.data.Gossip;
import dsProj1.msg.data.RetrieveMessage;

public class Node {
	public final @NonNull NodeStat ns;

	public Node(@NonNull UUID id, 
				@NonNull Collection<@NonNull UUID> initialView) {
		if (initialView.size() > Options.VIEWS_SIZE) {
			throw new IllegalArgumentException("initialView parameter can't be larger than " + Options.VIEWS_SIZE);
		}

		this.id = id;
		initialView.stream().map((UUID node) -> new Frequency(node)).forEach(this.view::add);

		this.ns = new NodeStat(this);
	}
	
	// TODO: Fare diagrammi articolo + magari qualcosina
	
	@NonNull List<@NonNull Frequency<UUID>> view = new ArrayList<@NonNull Frequency<UUID>>(2*Options.VIEWS_SIZE);              					   // Individual view // TODO: Fix permission(package -> private)
	private @NonNull List<@NonNull UUID> unSubs = new ArrayList<@NonNull UUID>(2*Options.UN_SUBS_SIZE);  // Un-subscriptions
	private @NonNull List<@NonNull Frequency<UUID>> subs = new ArrayList<@NonNull Frequency<UUID>>(2*Options.SUBS_SIZE);       // Subscriptions
	private @NonNull List<@NonNull EventId> eventIds	= new ArrayList<@NonNull EventId>(2*Options.EVENT_IDS_SIZE);      	   // Events already handled
	private @NonNull List<@NonNull ToRetrieveEv> retrieveBuf = new ArrayList<@NonNull ToRetrieveEv>();  // Events to be retrieved

	private @NonNull List<@NonNull Event> events = new ArrayList<@NonNull Event>(2*Options.EVENTS_SIZE);
		
	private @NonNull List<@NonNull Event> localEvents = new ArrayList<@NonNull Event>((int) (2*Options.EVENTS_RATE));
	
	public @NonNull UUID id;
	private long nextEventId = 0;

	public boolean alive = true;
	public long currentRound = 1;
	
	public static <T> Frequency<T> selectProcess(List<Frequency<T>> l) {
		double avg = ((double) l.stream().collect(Collectors.summingLong(w -> w.frequency))) / ((double) l.size());
		
		while (true) {
			Frequency<T> target = l.get(RandomHelper.nextIntFromTo(0, l.size()-1));
		
			if (target.frequency > Options.K * avg)
				return target;
			else
				++target.frequency;
		}
	}
	
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
					 						 .filter( id -> id.source.equals(eId.source))
					 						 .min( Comparator.comparing(id -> id.id) );

		return min.isPresent() && eId.id < min.get().id;
	}
	
	private void addEventId(EventId eId) {
		if (this.alreadySeenEventId(eId)) { 
			return; // If we have already seen this id, ignore
		}

		this.eventIds.add(eId);

		EventId min = this.eventIds
						  .stream()
					      .filter(id -> id.source.equals(eId.source))
					      .min(Comparator.comparing(id -> id.id))
					      .get(); // Guaranteed to get as I just added eId
		
		final long newId;
		
		{
			long toSearchId = min.id;
			for(;this.eventIds.contains(new EventId(eId.source, toSearchId+1));++toSearchId);

			newId = toSearchId;
		}
		
		this.eventIds.removeIf(id -> eId.source.equals(id.source) && newId > id.id);
	}
	
	private void trimEventIds() {
		if (this.eventIds.size() <= Options.EVENT_IDS_SIZE) 
			return;
		
		if (Options.EVENT_IDS_OPTIMIZATION_FIRST.equals("NONE")) {
			// Do nothing
		} else {
			Map<UUID, ArrayList<EventId>> sourceToIds = this.eventIds.stream().collect(Collectors.groupingBy(
					(EventId e) -> e.source, Collectors.toCollection(ArrayList::new)
			));
		
			ArrayList<List<EventId>> groupedList = sourceToIds.values().stream().map((ArrayList<EventId> evs) -> {
				evs.sort(Comparator.comparing((EventId e) -> -e.id));
				return evs;
			}).collect(Collectors.toCollection(ArrayList::new));
			
			int l = this.eventIds.size() - Options.EVENT_IDS_SIZE;

			if (Options.EVENT_IDS_OPTIMIZATION_FIRST.equals("UNIFORM")) {
				for (int i = 0; l>0 && !groupedList.isEmpty();) { // Remove uniformly
					List<EventId> k = groupedList.get(i % groupedList.size());
					
					if (k.size() == 0 || k.get(0).id - k.get(k.size()-1).id <= Options.LONG_AGO) {
						groupedList.remove(i % groupedList.size());
					} else {
						k.remove(k.size()-1);
						--l;
						
						while (l > 0 &&
							   k.get(0).id - k.get(k.size()-1).id > Options.LONG_AGO &&
							   k.get(k.size()-2).id == k.get(k.size()-1).id+1) {
							k.remove(k.size()-1);
							--l;
						}
						
						++i;
					}
				}
			} else { // Options.EVENT_IDS_OPTIMIZATION_FIRST.equals("SIZE")
				for (; l>0;) {
					Optional<List<EventId>> k = groupedList.stream()
												.map(w -> {
													int b = 0;
													for (;b<w.size() && w.get(0).id - w.get(b).id <= Options.LONG_AGO;++b);
													
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
					
					while (l > 0 && evs.size() > 1 && evs.get(evs.size()-2).id == evs.get(evs.size()-1).id+1) {
						evs.remove(evs.size()-1);
						--l;
					}
				}				
			}
			
			this.eventIds.clear();
			sourceToIds.values().stream().flatMap(k -> k.stream()).forEach(this::addEventId); // Checks are not needed but done anyway
		}

		// If more elements remain delete so to have this.eventIds.size() <= Options.EVENT_IDS_SIZE
		while (this.eventIds.size() > Options.EVENT_IDS_SIZE) {
			if (Options.EVENT_IDS_OPTIMIZATION_SECOND.equals("TIMESTAMP")) {
				// --- TIMESTAMP METHOD
				EventId e = this.eventIds.stream().collect(Collectors.minBy(Comparator.comparing((EventId eId) -> eId.timestamp))).get();
				this.eventIds.remove(e);
			} else { // Options.EVENT_IDS_OPTIMIZATION_SECOND.equals("RANDOM")
				// --- RANDOM METHOD
				this.eventIds.remove(RandomHelper.nextIntFromTo(0, this.eventIds.size()-1));
			}			

			List<EventId> tmp = new ArrayList<EventId>(this.eventIds);
			this.eventIds.clear();
			
			tmp.forEach(this::addEventId);
		}
	}
	
	private void trimEvents() {		
		if (this.events.size() <= Options.EVENTS_SIZE)
			return;
		
		if (Options.EVENTS_OPTIMIZATION_FIRST.equals("NONE")) {
			// Do nothing
		} else {
			Map<UUID, ArrayList<Event>> sourceToIds = this.events.stream().collect(Collectors.groupingBy(
					(Event e) -> e.eventId.source, Collectors.toCollection(ArrayList::new)
			));
			
			ArrayList<List<Event>> groupedList = sourceToIds.values().stream().map((ArrayList<Event> evs) -> {
				evs.sort(Comparator.comparing((Event e) -> -e.eventId.id));
				return evs;
			}).collect(Collectors.toCollection(ArrayList::new));
			
			int l = this.events.size() - Options.EVENTS_SIZE;

			if (Options.EVENTS_OPTIMIZATION_FIRST.equals("UNIFORM")) {
				for (int i = 0; l>0 && !groupedList.isEmpty();) { // Remove uniformly
					List<Event> k = groupedList.get(i % groupedList.size());
					
					if (k.size() == 0 || k.get(0).eventId.id - k.get(k.size()-1).eventId.id <= Options.LONG_AGO) {
						groupedList.remove(i % groupedList.size());
					} else {
						k.remove(k.size()-1);
						--l;
						++i;
					}
				}
			} else { // Options.EVENTS_OPTIMIZATION_FIRST.equals("SIZE")
				for (; l>0;) { // Remove uniformly
					Optional<List<Event>> k = groupedList.stream()
												.map(w -> {
													int b = 0;
													for (;b<w.size() && w.get(0).eventId.id - w.get(b).eventId.id <= Options.LONG_AGO;++b);
													
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
				}
			}
			
			this.events.clear();
			sourceToIds.values().stream()
					   			.flatMap(k -> k.stream())
					   			.forEach(this.events::add);
		}
	
		// Now that we removed the out-of-date items we can just get the first Options.EVENTS_SIZE with the smallest age
		while (this.events.size() > Options.EVENTS_SIZE) {
			if (Options.EVENTS_OPTIMIZATION_SECOND.equals("TIMESTAMP")) {
				// --- TIMESTAMP METHOD
				System.out.println("Timestamp  events");
				Event e = this.events.stream()
						 .collect(Collectors.minBy(Comparator.comparing((Event ev) -> ev.eventId.timestamp)))
						 .get();

				this.events.remove(e);
			} else {
				if (Options.EVENTS_OPTIMIZATION_SECOND.equals("AGE")) {
					// --- AGE METHOD
					System.out.println("Age  events");
					Event e = this.events.stream()
							 .collect(Collectors.maxBy(Comparator.comparing((Event ev) -> ev.age)))
							 .get();

					this.events.remove(e);
				} else { // Options.EVENTS_OPTIMIZATION_SECOND.equals("RANDOM")
					// --- RANDOM METHOD
					System.out.println("Random events");
					this.events.remove(RandomHelper.nextIntFromTo(0, this.events.size()-1));
				}
			}
		}
	}
	
	public void handleMessage(Message msg) throws Exception {
		if (msg.data instanceof RoundStart) {
			this.startRound();
		} else if (msg.data instanceof ExternalData) {
			Event e = new Event<>(this.id, this.nextEventId++, ((ExternalData) msg.data).data);
			e.eventId.timestamp = this.ns.getCurrentTimestamp();
			
			this.lpbCast(e);
			this.ns.stat(e);
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
		// PHASE 0: Cleanup
		// Remove data if it's about us
		g.data.events.removeIf(e -> e.eventId.source.equals(this.id));
		g.data.eventIds.removeIf(e -> e.source.equals(this.id));
		g.data.subs.remove(this.id);
		g.data.unSubs.remove(this.id);
		
		// PHASE 1: UPDATE VIEW AND UNSUBS
		// First let's convert subs in a list with frequencies
		List<Frequency<UUID>> newSubs = g.data.subs
											  .stream()
											  .map(u -> new Frequency<>(u))
											  .collect(Collectors.toCollection(ArrayList<Frequency<UUID>>::new));
		
		this.view.removeIf(f -> g.data.unSubs.contains(f.data)); // Remove all gossip unsubs from global view
		this.subs.removeIf(f -> g.data.unSubs.contains(f.data)); // Remove all gossip unsubs from global subs
		
		g.data.unSubs.stream()
				     .filter((UUID it) -> !this.unSubs.contains(it)) // Remove already contained
				     .forEach(this.unSubs::add);     				 // Add unsubs from gossip to local unsubs
				
		// Randomly select UN_SUBS_SIZE values from unsub, this is the new unsubs		
		shuffleTrim(this.unSubs, Options.UN_SUBS_SIZE);
		
		// PHASE 2: ADD NEW SUBSCRIPTIONS	
		if (Options.SUBS_OPTIMIZATION) {
			newSubs.forEach(m -> {
				int idx = this.view.indexOf(m);
				
				Frequency<UUID> m_ = idx != -1? this.view.get(idx) : m;
				
				if (idx == -1) 
					this.view.add(m_);

				++m_.frequency;
			});
			
			newSubs.forEach(m -> {
				int idx = this.subs.indexOf(m);
				
				Frequency<UUID> m_ = idx != -1? this.subs.get(idx) : m;
				
				if (idx == -1) 
					this.subs.add(m_);

				++m_.frequency;				
			});
			
			while (this.view.size() > Options.VIEWS_SIZE) {
				Frequency<UUID> target = selectProcess(this.view);
				this.view.remove(target);
				
				if (!this.subs.contains(target))
					this.subs.add(target);
			}
			
			while (this.subs.size() > Options.SUBS_SIZE) {
				Frequency<UUID> target = selectProcess(this.subs);
				subs.remove(target);
			}
		} else {
			newSubs.stream()
		  	   .filter((Frequency<UUID> it) -> !this.view.contains(it) && !it.equals(this.id))
		  	   .forEach((Frequency<UUID> it) -> {
		  		   if (!this.subs.contains(it)) this.subs.add(it);
				 
				   this.view.add(it);
		  	   });

			if (this.view.size() > Options.VIEWS_SIZE) {
				Collections.shuffle(this.view);
				List<Frequency<UUID>> t = this.view.subList(Options.VIEWS_SIZE, this.view.size());
				this.subs.removeAll(t);
				t.clear();
			}

			shuffleTrim(this.subs, Options.SUBS_SIZE);
		}
		
		// PHASE 3: UPDATE EVENTS WITH NEW NOTIFICATIONS	
		// Update ages	
		g.data.events
			  .stream()
			  .forEach((Event e) -> {
				  int idx = this.events.indexOf(e);
				  
				  if (idx == -1) {
					  return;
				  }
				  
				  Event found = this.events.get(idx);
				  found.age = Math.max(found.age, e.age);
			  });
		
		// Handle all new events
		g.data.events
			  .forEach(this::handleEvent);
		
		// Retrieve all lost events
		g.data.eventIds
		 	  .stream()
		 	  .filter( (EventId it) -> !this.alreadySeenEventId(it))
		 	  .map( (EventId it) -> new ToRetrieveEv(it, this.currentRound, g.source))
			  .forEach( k -> {
				  if (this.retrieveBuf.contains(k))
					  return;
				  
				  this.retrieveBuf.add(k);
				  
			  });
				
		Map<UUID, Optional<@NonNull EventId>> evs = this.eventIds.stream()
					 											 .collect(
					 													Collectors.groupingBy( (EventId e) -> e.source,		 
					 													Collectors.minBy( Comparator.comparing((EventId e) -> e.id) )));
		
		g.data.eventIds.stream()
					   .collect(Collectors.groupingBy((EventId e) -> e.source,
							    					  Collectors.minBy( Comparator.comparing((EventId e) -> e.id))))
					   .values()
					   .stream()
					   .forEach( (Optional<EventId> oEv) -> {
						   EventId ev = oEv.get();
						   
						   if (evs.containsKey(ev.source)) {
							   EventId toComp = evs.get(ev.source).get();
							   
							   for(long i = ev.id-1; i > toComp.id; --i) {
								   ToRetrieveEv toRetrieveEv = new ToRetrieveEv(new EventId(ev.source, i), this.currentRound, g.source);
								   
								   if (!this.retrieveBuf.contains(toRetrieveEv)) {
									   this.retrieveBuf.add(toRetrieveEv);								   
								   }
							   }
						   }
					   });

		this.trimEventIds();
		this.trimEvents();
	}

	private void requestRetrieve(@NonNull EventId eventId, @NonNull UUID dest) {
		RetrieveMessage rm = new RetrieveMessage(eventId);
		this.send(dest, rm);
	}

	private void send(@NonNull UUID destination, @NonNull Object data) {
		this.ns.send(new Message<>(this.id, destination, data));
	}

	private void scheduleGossip(double delay) {
		this.ns.scheduleGossip(delay,  new Message<RoundStart>(this.id, this.id, new RoundStart()));
	}
	
	public void lpbCast(@NonNull Event e) { // TODO: To integrate in some way with sending
		this.ns.stat(e);
		this.localEvents.add(e);
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
		Optional<Event> oe = this.events.stream()
				   						.filter(e -> e.eventId.equals(rm.data.eventIdRequested))
				   						.findAny();

		if (!oe.isPresent())
			return;

		this.send(rm.source, oe.get());
	}

	private void lpbDeliver(@NonNull Event ev) {
		this.ns.stat(ev);
	}

	private Gossip emitGossip() {
		// Select FANOUT_SIZE targets to which send the gossip
		List<@NonNull Frequency<UUID>> targets = new ArrayList<>(Options.FANOUT_SIZE);
		while (targets.size() <= Math.min(this.view.size()-1, Options.FANOUT_SIZE)) {
			Frequency<UUID> w = this.view.get(RandomHelper.nextIntFromTo(0, this.view.size()-1));
			
			if (!targets.contains(w)) {
				targets.add(w);
			}
		}

		// Add ourselves to the subs list (if there is enough space just add, otherwise substitute at random)
		List<@NonNull UUID> s = this.subs.stream().map(f -> f.data).collect(Collectors.toCollection(ArrayList<UUID>::new));
		if (s.size() < Options.SUBS_SIZE)
			s.add(this.id);
		else 
			s.set(RandomHelper.nextIntFromTo(0, this.subs.size()-1), this.id);

		this.events.addAll(this.localEvents);
		this.localEvents.clear();
		
		// Create the gossip and sent it
		Gossip g = new Gossip(s, this.unSubs, this.eventIds, this.events);
		targets.forEach(t -> this.send(t.data, g));

		//this.events.clear();

		// Schedule another gossip in Options.GOSSIP_INTERVAL seconds
		this.scheduleGossip(Options.GOSSIP_INTERVAL);
		
		return g;
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
				if (Options.RETRIEVE_METHOD.equals("MINE")) {
					switch(el.noRequests) {
						case -1:
							List<@NonNull UUID> targets = new ArrayList<>(Options.FANOUT_SIZE);
							targets.add(el.sender);
							while (targets.size() <= Math.min(this.view.size()-1, Options.FANOUT_SIZE)) {
								UUID w = this.view.get(RandomHelper.nextIntFromTo(0, this.view.size()-1)).data;
								
								if (!targets.contains(w)) {
									targets.add(w);
								}
							}
							
							targets.forEach(k -> this.requestRetrieve(el.eventId, k));
							el.noRequests++;
							el.requestedAtRound = this.currentRound;
							break;
						case 0:
							if (this.currentRound >= el.requestedAtRound + Options.REQUEST_TIMEOUT_ROUNDS ) {
								this.requestRetrieve(el.eventId, el.eventId.source); // How to get the source?
								el.noRequests++;
								el.requestedAtRound = this.currentRound;
							}
							break;
						case 1:
							if (this.currentRound >= el.requestedAtRound + Options.REQUEST_TIMEOUT_ROUNDS ) {
								// Packet is considered lost, log something and give up
								this.ns.retrieveFail(el.eventId);
								it.remove();
							}
							break;
					}
				} else {
					switch(el.noRequests) {
						case -1: // If is the first time this happens ask to who sent us the related gossip
							this.requestRetrieve(el.eventId, el.sender);
							el.noRequests++;
							el.requestedAtRound = this.currentRound;
							break;
						case 0: // If it's the second time ask to a random node
							if (this.currentRound >= el.requestedAtRound + Options.REQUEST_TIMEOUT_ROUNDS ) {
								this.requestRetrieve(el.eventId, this.view.get( RandomHelper.nextIntFromTo(0, this.view.size()-1) ).data);
								el.noRequests++;
								el.requestedAtRound = this.currentRound;
							}
							break;
						case 1: // If it's the third time ask source
							if (this.currentRound >= el.requestedAtRound + Options.REQUEST_TIMEOUT_ROUNDS ) {
								this.requestRetrieve(el.eventId, el.eventId.source); // How to get the source?
								el.noRequests++;
								el.requestedAtRound = this.currentRound;
							}
							break;
						case 2: // If 3° time failed just fail
							if (this.currentRound >= el.requestedAtRound + Options.REQUEST_TIMEOUT_ROUNDS ) {
								// Packet is considered lost, log something and give up
								this.ns.retrieveFail(el.eventId);
								it.remove();
							}
							break;
					}
				}
			}
		}
	}
	
	public void startRound() {
		++this.currentRound;
		this.events.forEach(e -> ++e.age);
		Gossip g = this.emitGossip();
		this.retrieve();
	}
}
