package dsProj1;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.graph.Network;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import dsProj1.msg.DummyStartGossip;
import dsProj1.msg.Event;
import dsProj1.msg.GenericMessage;
import dsProj1.msg.Gossip;;

public class Node {
	@NonNull
	private ContinuousSpace<Object> space;

	@NonNull
	private Grid<Object> grid;

	@NonNull
	private Oracle oracle;

	public Node(@NonNull ContinuousSpace<Object> space, 
			@NonNull Grid<Object> grid, 
			@NonNull UUID id, 
			@NonNull Collection<UUID> initialView, 
			@NonNull Oracle oracle) {
		if (initialView.size() > Options.VIEWS_SIZE) {
			throw new IllegalArgumentException("initialView parameter can't be larger than " + Options.VIEWS_SIZE);
		}
		
		this.space = space;
		this.grid = grid;
		
		this.id = UUID.fromString(id.toString());
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

	
	@NonNull
	public UUID id;

	public boolean state = false;
	private long currentRound = 1;

	
	static void shuffle_trim(@NonNull List l, int dim) {
		if (l.size() > dim) {
			Collections.shuffle(l);
			l.subList(dim, l.size()).clear();			
		}
	}
	
	public void handle_gossip(@NonNull Gossip g) {
		// PHASE 1: UPDATE VIEW AND UNSUBS
		view.removeAll(g.unSubs); // Remove all gossip unsubs from global view
		subs.removeAll(g.unSubs); // Remove all gossip unsubs from global subs
		
		g.unSubs.forEach( (UUID it) -> { // Add (not already contained) gossip unsubs to global unsubs
			if (!unSubs.contains(it)) unSubs.add(it);
		});
				
		// Randomly select UN_SUBS_SIZE values from unsub, this is the new unsubs		
		shuffle_trim(unSubs, Options.UN_SUBS_SIZE);
		
		// PHASE 2: ADD NEW SUBSCRIPTIONS
		g.subs.forEach((UUID it )-> { // TODO: Check variables
			System.out.println("SUBS IN NOT EMPTY!");
			if (!this.view.contains(it) && !it.equals(this.id) ) {
				System.out.println("NEWSUB");
				if (!this.subs.contains(it)) this.subs.add(it);
				 
				this.view.add(it);	
			}
		});
		
		if (this.view.size() > Options.VIEWS_SIZE) {
			Collections.shuffle(this.view);
			List<UUID> t = this.view.subList(Options.VIEWS_SIZE, this.view.size());
			subs.removeAll(t);
			t.clear();
		}
		
		shuffle_trim(this.subs, Options.SUBS_SIZE);
		
		// PHASE 3: UPDATE EVENTS WITH NEW NOTIFICATIONS
		g.events.forEach( (Event ev) -> handleEvent(ev) ); // Handle all new events
		
		g.eventIds.forEach( (UUID it) -> { // Add each new eventId to the ones to retrieve separetly
			if (!eventIds.contains(it)) {
				retrieveBuf.add(new ToRetrieveEv(it, currentRound, g.process_id));
			}
		});
		
		shuffle_trim(this.eventIds, Options.EVENT_IDS_SIZE); // TODO:  This is to optimize using timestamps
		shuffle_trim(this.events, Options.EVENTS_SIZE);
	}
	
	public void requestRetrieve(@NonNull UUID eventId,@NonNull UUID dest) {
		// TODO: Da fare
	}
	
	public void send(@NonNull UUID dest, @NonNull GenericMessage g) {
		g.process_id = this.id;
		g.dest = dest;

		this.oracle.send(g);
	}
	
	public void scheduleGossip() {
		this.oracle.scheduleGossip(Options.GOSSIP_INTERVAL, new DummyStartGossip(this.id));
	}
	
	public void lpbCast(@NonNull Event e) {
		if (!this.events.contains(e)) { // TODO: Check equals here
			this.events.add(e);			
		}
	}
	
	public void handleEvent(@NonNull Event ev) {
		if (!eventIds.contains(ev.id)) {
			if (!events.contains(ev)) {
				events.add(ev);
			}
			
			lpbDeliver(ev);
			
			eventIds.add(ev.id);
		}
	}
	
	public void lpbDeliver(@NonNull Event e) {
		// TODO: Da fare, cambia come virtuale puro da fare override
		// (this is an application override)
	}

	public void emitGossip() {
		Random rand = new Random();
				
		List<UUID> targets = new ArrayList<UUID>(Options.FANOUT_SIZE);

		while (targets.size() < Options.FANOUT_SIZE) {
			UUID w = this.view.get(rand.nextInt(this.view.size()));
			
			if (!targets.contains(w)) {
				targets.add(UUID.fromString(w.toString()));
			}
		}

		List<UUID> s = new ArrayList<>(this.subs);
		if (s.isEmpty())
			s.add(this.id);
		else 
			s.set(rand.nextInt(this.subs.size()), this.id);
		
		Gossip g = new Gossip(this.unSubs, s, this.eventIds, this.events);
		
		
		System.out.println("TARGETS" + targets);
		//targets.forEach(t -> this.send(t, g));
		for(UUID t: targets) {
			this.send(t, g);
		}
		 
		this.events.clear(); // TODO: DUFAQ

		// -- Schedule for next time
		this.scheduleGossip();
	}

	public void retrieve() {
		this.retrieveBuf.forEach((ToRetrieveEv el) -> {
			if (this.currentRound - el.round <= Options.OLD_TIME_RETRIEVE) {
				this.retrieveBuf.remove(el);
				return; // Not enough time passed for worrying
			}		

			if (!this.eventIds.contains(el.event_id)) {
				if(el.noRequests == -1) { // If is the first time this happens ask to who sent us the related gossip
					this.requestRetrieve(el.event_id, el.sender);
					el.noRequests++;
					el.requestedAtRound = this.currentRound;
				} else if(el.noRequests == 1) { // If it's the second time ask at random
					if (this.currentRound > el.requestedAtRound + Options.REQUEST_TIMEOUT_ROUNDS ) {
						this.requestRetrieve(el.event_id, this.view.get(new Random().nextInt(this.view.size())));
						el.noRequests++;
						el.requestedAtRound = this.currentRound;
					}
				} else if(el.noRequests == 2) { // If it's the third time ask source
//					if (this.currentRound > el.requestedAtRound + Options.REQUEST_TIMEOUT_ROUNDS ) {
//						this.requestRetrieve(el.event_id, el.source); // How to get the source?
//						el.noRequests++;
//						el.requestedAtRound = this.currentRound;
//					}
//				} else if (el.noRequests == 3) { // If 3° time failed just fail
					if (this.currentRound > el.requestedAtRound + Options.REQUEST_TIMEOUT_ROUNDS ) {
						// Packet is considered lost, log something
					}
				}				
			}
		});
	}
		
	//@ScheduledMethod(start=1, interval=1)
	public void step() throws Exception { // We consider each step as a round(synchronous rounds)
		this.retrieve();
		this.emitGossip();
				
		this.currentRound++;
	}
}
