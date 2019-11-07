package dsProj1;

// Support libraries
import org.eclipse.jdt.annotation.NonNull;

// Standard libraries
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

// Custom libraries
import dsProj1.msg.Message;
import dsProj1.msg.data.DummyStartGossip;
import dsProj1.msg.data.Event;
import dsProj1.msg.data.Gossip;

// TODO: Decide when to call retrieve()
// TODO: Decide when to change currentRound

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
	
	// Fare diagrammi articolo + magari qualcosina
	
	// GOALS
	// event notifications
	// event notifications identifiers
	// un-subscriptions
	// subscriptions
	
	// BUFFERS
	// view        (neibourghs)
	// unSubs      (unsubscriptions)
	// subs        (new subscriptions)
	// eventIds    (ids of already received events)
	// retrieveBuf (we put events here to be processed)
	
	// PHASES
	// 1. Handle unsubscriptions
	//    1.1 remove unsubs from view
	//    1.2 size-limit unsubs (randomly)
	// 2. Handle subscriptions+
	//    2.1 add subscriptions to view (pay attention to duplicates)
	//    2.2 limit view (randomly)
	//    2.3 limit subscription (randomly)
	// 3. Handle deliveries
	//    3.1 Remove deliveries that we already processed (should not be in eventIds)
	//    3.2 Put new deliveries in retrieveBuf
	
	// THINGS
	// - Every T generate gossip message to gossip to F random other processes
	
	// For each el in retrieveBuf, check if we have waited enough (k rounds) before fetching data (from who told me about the event)
	// Then if during waiting period the event notification was received in a subsequent gossip message. If not ask for event notification
	
	@NonNull List<@NonNull UUID> view = new ArrayList<@NonNull UUID>();//2*Options.VIEWS_SIZE);              // Individual view // TODO: Fix permission(package -> private)
	private @NonNull List<@NonNull UUID> unSubs = new ArrayList<@NonNull UUID>();//2*Options.UN_SUBS_SIZE);          // Un-subscriptions
	private @NonNull List<@NonNull UUID> subs = new ArrayList<@NonNull UUID>();//2*Options.SUBS_SIZE);               // Subscriptions
	private @NonNull List<@NonNull UUID> eventIds	= new ArrayList<@NonNull UUID>();//2*Options.EVENT_IDS_SIZE);      // Events already handled
	private @NonNull List<@NonNull ToRetrieveEv> retrieveBuf = new ArrayList<@NonNull ToRetrieveEv>();//2*Options.RETRIEVE_SIZE);  // Events to be retrieved

	private @NonNull List<@NonNull Event> events = new ArrayList<@NonNull Event>(); // [EVENTS_SIZE]

	
	public @NonNull UUID id;

	public boolean alive = true;
	private long currentRound = 1;

	
	static void shuffle_trim(@NonNull List<?> l, int dim) {
		if (l.size() <= dim) 
			return;
		
		Collections.shuffle(l);
		l.subList(dim, l.size()).clear();			
	}
	
	public void handle_gossip(@NonNull Message<Gossip> g) {
		// PHASE 1: UPDATE VIEW AND UNSUBS
		view.removeAll(g.data.unSubs); // Remove all gossip unsubs from global view
		subs.removeAll(g.data.unSubs); // Remove all gossip unsubs from global subs
		
		g.data.unSubs.stream()
				     .filter((UUID it) -> !unSubs.contains(it)) // Remove already contained
				     .forEach(unSubs::add);     				// Add unsubs from gossip to local unsubs
				
		// Randomly select UN_SUBS_SIZE values from unsub, this is the new unsubs		
		shuffle_trim(unSubs, Options.UN_SUBS_SIZE);
		
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
		
		shuffle_trim(this.subs, Options.SUBS_SIZE);
		
		// PHASE 3: UPDATE EVENTS WITH NEW NOTIFICATIONS
		g.data.events.forEach(this::handleEvent); // Handle all new events
		
		g.data.eventIds.stream()
				  .filter( (UUID it) -> !this.eventIds.contains(it) )
				  .forEach( (UUID it) -> {
					  retrieveBuf.add(new ToRetrieveEv(it, currentRound, g.source));
				  });
		
		shuffle_trim(this.eventIds, Options.EVENT_IDS_SIZE); // TODO:  This is to optimize using timestamps
		shuffle_trim(this.events, Options.EVENTS_SIZE);
	}
	
	public void requestRetrieve(@NonNull UUID eventId,@NonNull UUID dest) {
		// TODO: Da fare
	}
	
	public void send(@NonNull UUID destination, @NonNull Object data) {
		Message<Object> msg = new Message<Object>(this.id, destination, data);

		this.oracle.send(msg);
	}
	
	public void scheduleGossip(double delay) {
		Message<DummyStartGossip> msg = new Message<DummyStartGossip>(this.id, 
																	  this.id, 
																	  new DummyStartGossip());
		
		this.oracle.scheduleGossip(delay, msg);
	}
	
	public void lpbCast(@NonNull Event e) { // TODO: To integrate in some way with sending
		// If the event was already (completely/in full) received ignore
		if (this.events.contains(e))
			return;
		
		// Otherwise, add
		this.events.add(e);
	}
	
	public void handleEvent(@NonNull Event ev) {
		// If the event was already (completely) received ignore
		if (this.events.contains(ev))
			return;
		
		this.events.add(ev);

		// Deliver the event to the application
		this.lpbDeliver(ev);

		// Add the id of the event
		this.eventIds.add(ev.eventId);
	}
	
	public void lpbDeliver(@NonNull Event ev) {
		// TODO: Da fare, cambia come virtuale puro, da fare override
		// (this is an application override)
	}

	public void emitGossip() {
		Random rand = new Random();
			
		// Select FANOUT_SIZE targets to which send the gossip
		List<UUID> targets = new ArrayList<UUID>(Options.FANOUT_SIZE);
		while (targets.size() < Options.FANOUT_SIZE) {
			UUID w = this.view.get(rand.nextInt(this.view.size()));
			
			if (!targets.contains(w)) {
				targets.add(w);
			}
		}

		// Add ourselves to the subs list (if there is enough space just add, otherwise substitute at random)
		List<UUID> s = new ArrayList<>(this.subs);
		if (s.size() < Options.SUBS_SIZE)
			s.add(this.id);
		else 
			s.set(rand.nextInt(this.subs.size()), this.id);
		
		// Create the gossip and sent it
		Gossip g = new Gossip(this.unSubs, s, this.eventIds, this.events);
		targets.forEach(t -> this.send(t, g));
				 
		this.events.clear(); // TODO: DUFAQ

		// Schedule another gossip in Options.GOSSIP_INTERVAL seconds
		this.scheduleGossip(Options.GOSSIP_INTERVAL);
	}

	public void retrieve() {
		this.retrieveBuf.forEach((ToRetrieveEv el) -> {
			// If it's still too early, don't worry
			if (this.currentRound - el.round <= Options.OLD_TIME_RETRIEVE) {
				this.retrieveBuf.remove(el);
				return; 
			}		

			// ??
			if (this.eventIds.contains(el.eventId))
				return;
			
			switch((int)el.noRequests) {
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
//					if (this.currentRound > el.requestedAtRound + Options.REQUEST_TIMEOUT_ROUNDS ) {
//						this.requestRetrieve(el.event_id, el.source); // How to get the source?
//						el.noRequests++;
//						el.requestedAtRound = this.currentRound;
//					}
//					break;
//				case 2: // If 3° time failed just fail
					if (this.currentRound > el.requestedAtRound + Options.REQUEST_TIMEOUT_ROUNDS ) {
						// Packet is considered lost, log something
					}
					break;

			}
		});
	}
}
