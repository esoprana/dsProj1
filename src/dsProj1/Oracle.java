package dsProj1;

// Support libraries
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
// Standard libraries
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

// Repast libraries
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.Network;
import repast.simphony.util.ContextUtils;

// Custom libraries
import dsProj1.msg.Message;
import dsProj1.msg.data.RoundStart;
import dsProj1.msg.data.Event;
import dsProj1.msg.data.ExternalData;
import dsProj1.msg.data.Gossip;
import dsProj1.msg.data.RetrieveMessage;

public class Oracle {
	private double currentSeconds = 0;
	private long nData = 0;
	
	private HashMap<UUID, HashMap<Long, Long>> received = new HashMap<>();
	
	public void stat(UUID node, Event ev) {
		received.putIfAbsent(node, new HashMap<>());
		
		HashMap<Long, Long> mapOfNode = received.get(node);
		mapOfNode.putIfAbsent((Long) ev.data, 0L);
		
		Long w = mapOfNode.get((Long) ev.data);
		w++;
	}
	
	private static double normal(double mean, double var) {
		return RandomHelper.createNormal(mean, var)
		 				   .apply(RandomHelper.nextDoubleFromTo(0, 1));
	}
	
	private static double normalCut(double mean, double var) {
		double ris = normal(mean, var);

		return ris < 0?0:ris;
	}
	
	@NonNull TreeSet<@NonNull Timestamped<Message<?>>> messages = new TreeSet<@NonNull Timestamped<Message<?>>>();
	
	public void send(@NonNull Message<?> msg) {
		double delayTo = normalCut(Options.MEAN_LATENCY, Options.VAR_LATENCY);
		messages.add(new Timestamped<Message<?>>(currentSeconds+delayTo, msg));
	}
	
	public void scheduleGossip(double delay, @NonNull Message<RoundStart> dg) {
		// TODO: Add clock drift?
		double delayTo = normalCut(delay, delay*Options.DRIFT_PER_SECOND);
		messages.add(new Timestamped<Message<?>>(currentSeconds+delayTo, dg));
	}
	
	private @Nullable Node getNode(@NonNull UUID id) {
		Context context = ContextUtils.getContext(this);
		for(Object o : context.getObjects(Node.class)) {
			Node n = (Node) o;
			
			if (n.id.equals(id))
				return n;
		}

		return null;
	}
	
	public String currentTask() {
		@Nullable Timestamped<Message<?>> msg = this.messages.first();
		
		if (msg == null)
			return "";
		
		return msg.toString();
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void step() throws Exception {
		// Print what you are about to do (debugging purposes)
		//System.out.println(this.currentTask());
		
		@Nullable Timestamped<Message<?>> msg = messages.pollFirst();
		
		// If nothing is scheduled something bad happened (there should be at least some periodic event)
		if(msg == null)
			throw new Exception("No more messages/events!");
		
		// From now on use copy (messages could be changed by node)	
		msg = new Timestamped<Message<?>>(msg);
		
		this.currentSeconds = msg.timestamp;

		Node sender = this.getNode(msg.message.source);
		Node destination = this.getNode(msg.message.destination);		

		// If there is no destination something bad happened (the node should never be removed from the context, use DEAD status instead)
		if (destination == null) {
			throw new Exception("Destination of message is null!");
		} else if (sender == null) {
			throw new Exception("Source of message is null!");
		}
		
		boolean toBeReceived = true;
		
		{
			if (msg.message.data instanceof Gossip) {
				((Gossip)msg.message.data).events.forEach(e -> sender.lpbCast(e));
			}
		}
		
		// If destination is dead, update the view and exit immediately (do not use any handle)
		if (!destination.alive) {
			toBeReceived = false;
		}

		if (RandomHelper.nextDouble() <= Options.DROPPED_RATE) {
			toBeReceived = false;
		}
		
		if(toBeReceived)
			destination.handleMessage(msg.message);
				
		this.updateView(msg);
	}
	
	public void updateView(Timestamped<Message<?>> msg) {
		// Get source and destination (if scheduled event of node, source and destination are the same)
		Node source = this.getNode(msg.message.source);
		Node destination = this.getNode(msg.message.destination);
		
		// If source or destination are null, log and exit function
		if (source == null) {
			System.err.println("Error during visualization, message has as source a null value");
			return;
		} else if (destination == null) { 
			System.err.println("Error during visualization, message has as destination a null value");
			return;
		}
	
		// Get networks
		Context context = ContextUtils.getContext(this);
	
		Network<Node> networkView = (Network<Node>) context.getProjection("view");
		Network<Node> networkMessage = (Network<Node>) context.getProjection("message");

		// Remove old edges
		networkView.removeEdges();
		networkMessage.removeEdges();

		// - View network
		// Create a map (id: UUID -> node: Node)
		HashMap<UUID, Node> nodes = new HashMap<UUID, Node>();
		context.getObjects(Node.class).forEach((Object o) -> {
			Node n = (Node) o;
			nodes.put(n.id, n);
		});
		
		source.view
	 	      .stream()
	 	      .map(nodes::get)
	 	      .forEach( (Node to) -> networkView.addEdge(source, to) );
		
		// - Message network
		networkMessage.addEdge(source, destination);
	}

	public void init(Context ctx) {
		cern.jet.random.Uniform u = RandomHelper.createUniform(0, Options.TO_SECOND);

		List<Node> nodes = new ArrayList<Node>(Options.NODE_COUNT);
		for (Object o : ctx.getObjects(Node.class)) {
			nodes.add((Node)o);
		}
		
		// Schedule its first gossip // TODO: Change timings (maybe random or something)
		nodes.stream().forEach( (Node n) -> {
			this.scheduleGossip(0, new Message<>(n.id, n.id, new RoundStart()));
		});

		long tot_events = (long) (Options.TO_SECOND * Options.EVENTS_RATE);
		ArrayList<Double> exData = new ArrayList<Double>((int) tot_events);

		for (int i=0; i<tot_events; ++i) {
			exData.add(u.nextDoubleFromTo(0, Options.TO_SECOND)+1);
		}
		
		exData.stream().sorted().map((Double t) -> {
			Node n = nodes.get(RandomHelper.nextIntFromTo(0, nodes.size()-1));
			Message msg = new Message<ExternalData<?>>(n.id, n.id, new ExternalData<>(++this.nData));
			
			return new Timestamped(t, msg);
		}).forEach(this.messages::add);
	}
}
