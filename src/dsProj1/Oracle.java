package dsProj1;

// Support libraries
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
// Standard libraries
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

// Repast libraries
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunListener;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.Network;
import repast.simphony.util.ContextUtils;
import zmq.Msg;
// Custom libraries
import dsProj1.msg.Message;
import dsProj1.msg.data.RoundStart;
import dsProj1.msg.data.Event;
import dsProj1.msg.data.ExternalData;
import dsProj1.msg.data.Gossip;
import dsProj1.msg.data.RetrieveMessage;

public class Oracle {
	public double currentSeconds = 0;
	public double lastScheduling = 0;
	
	private long nData = 0;
	
	private static double normal(double mean, double var) {
		return RandomHelper.createNormal(mean, var)
		 				   .apply(RandomHelper.nextDoubleFromTo(0, 1));
	}
	
	private static double normalCut(double mean, double var) {
		double ris = normal(mean, var);

		return ris < 0?0:ris;
	}
	
	
	/*
	private HashMap<UUID, HashMap<Long, Long>> received = new HashMap<>();
	
	public void stat(UUID node, Event ev) {
		received.putIfAbsent(node, new HashMap<>());
		
		HashMap<Long, Long> mapOfNode = received.get(node);
		
		Long dataId = (Long) ev.data;
		
		mapOfNode.put(dataId, mapOfNode.getOrDefault(dataId, 0L)+1);
	}
	
	public String getStat() throws IOException {
		java.io.File file = new java.io.File("stat.csv");
		java.io.FileWriter writer = new java.io.FileWriter(file, false);

		Set<Long> data = this.received.values().stream().flatMap(v -> v.keySet().stream()).collect(Collectors.toSet());

		String r = "";

		for (Map.Entry<UUID, HashMap<Long, Long>> e : this.received.entrySet()) {
			for (Long w : data) {
				writer.write(e.getKey() + ", " + w + ", " + e.getValue().getOrDefault(w, 0L) + "\n");
			}
		}
		
		writer.flush();

		return r;
	}*/
	
	@NonNull TreeSet<@NonNull Timestamped<Message<?>>> messages = new TreeSet<@NonNull Timestamped<Message<?>>>();
	
	public void send(@NonNull Message<?> msg) {
		double delayTo = normalCut(Options.MEAN_LATENCY, Options.VAR_LATENCY);
		messages.add(new Timestamped<Message<?>>(currentSeconds+delayTo, msg));
	}
	
	public void scheduleGossip(double delay, @NonNull Message<RoundStart> dg) {
		double delayTo = delay; //normalCut(delay, delay*Options.DRIFT_PER_SECOND);
		messages.add(new Timestamped<Message<?>>(currentSeconds+delayTo, dg));
	}
	
	private @Nullable NodeStat getNodeStat(@NonNull UUID id) {
		Context context = ContextUtils.getContext(this);
		for(Object o : context.getObjects(NodeStat.class)) {
			NodeStat ns = (NodeStat) o;
			
			if (ns.getId().equals(id))
				return ns;
		}

		return null;
	}
	
	public String currentTask() {
		try {
			Timestamped<Message<?>> msg = this.messages.first();
			
			return msg.toString();
		} catch (NoSuchElementException w) {
			return "";
		}
	}
	
	private void schedule() {
		if (((int)(this.nData+Options.EVENTS_RATE))/500 > this.nData/500) { // Every 500 events created write .csv
			System.out.println("write" + this.nData);
			Context c = ContextUtils.getContext(this);
			c.getObjects(NodeStat.class).forEach(ns -> ((NodeStat)ns).writeAll());			
		}
		
		// Update time to new schedule time
		this.lastScheduling+=Options.GOSSIP_INTERVAL;

		// Get all nodes
		Context ctx = ContextUtils.getContext(this);
		List<Node> nodes = new ArrayList<Node>(Options.NODE_COUNT);
		for (Object o : ctx.getObjects(NodeStat.class)) {
			NodeStat ns = (NodeStat) o;
			if (ns.getNode().alive) {
				if ( RandomHelper.nextDoubleFromTo(0, 1) < Options.DEATH_RATE_PER_INTERVAL )
					ns.die(this.currentSeconds);
				else
					nodes.add(ns.getNode());
			}
		}
		
		// Create new events
		cern.jet.random.Uniform u = RandomHelper.createUniform(0, Options.GOSSIP_INTERVAL);
		
		long tot_events = Math.round(normalCut(Options.EVENTS_RATE, Options.EVENTS_VAR_RATE));
		ArrayList<Double> exData = new ArrayList<Double>((int) tot_events);

		for (int i=0; i<tot_events; ++i) {
			exData.add(u.nextDouble() + this.lastScheduling);
		}
		
		exData.stream().sorted().map((Double t) -> {
			Node node = nodes.get(RandomHelper.nextIntFromTo(0, nodes.size()-1));
			Message msg = new Message<ExternalData<?>>(node.id, node.id, new ExternalData<>(++this.nData));
			
			return new Timestamped(t, msg);
		}).forEach(this.messages::add);
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void step() throws Exception {
		@Nullable Timestamped<Message<?>> msg = messages.pollFirst();
		
		// If nothing is scheduled something bad happened (there should be at least some periodic event)
		if(msg == null)
			throw new Exception("No more messages/events!");
		
		// From now on use copy (messages could be changed by node)	
		msg = new Timestamped<Message<?>>(msg);
		
		this.currentSeconds = msg.timestamp;

		NodeStat sender = this.getNodeStat(msg.message.source);
		NodeStat destination = this.getNodeStat(msg.message.destination);		

		// If there is no destination something bad happened (the node should never be removed from the context, use DEAD status instead)
		if (destination == null) {
			throw new Exception("Destination of message is null!");
		} else if (sender == null) {
			throw new Exception("Source of message is null!");
		}
		
		boolean toBeReceived = true;
		
		// If destination is dead, update the view and exit immediately (do not use any handle)
		if (!destination.getNode().alive) {
			toBeReceived = false;
		}
		
		// If the message is not a simulated one but a real one (no schedule but message)
		if (msg.message.data instanceof Event || 
			msg.message.data instanceof RetrieveMessage || 
			msg.message.data instanceof Gossip) {
			if (RandomHelper.nextDouble() < Options.DROPPED_RATE) {
				toBeReceived = false;
			}
		}
		
		if(toBeReceived)
			destination.handleMessage(msg.message);
				

		this.updateView(msg, toBeReceived);
		
		if (messages.isEmpty() || this.lastScheduling + Options.GOSSIP_INTERVAL <= messages.first().timestamp) {
			this.schedule();
		}
		
		if (Options.STOP_TIME != 0 && this.currentSeconds >= Options.STOP_TIME) {
			RunEnvironment.getInstance().endRun();
		}
	}
	
	public void updateView(Timestamped<Message<?>> msg, boolean toBeReceived) {
		// Get source and destination (if scheduled event of node, source and destination are the same)
		NodeStat source = this.getNodeStat(msg.message.source);
		NodeStat destination = this.getNodeStat(msg.message.destination);
		
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
		
		destination.getNode()
			  	   .view
	 	           .stream()
	 	           .forEach( (Frequency<UUID> v) -> {
	 	        	   networkView.addEdge(destination.getNode(), nodes.get(v.data), v.frequency + 1.); // Show frequency using width
	 	           });
		
		// - Message network
		networkMessage.addEdge(new MessageEdge(source.getNode(), destination.getNode(), msg.message, toBeReceived?1:0));
	}

	public void init(Context ctx) {
		List<Node> nodes = new ArrayList<Node>(Options.NODE_COUNT);
		for (Object o : ctx.getObjects(Node.class)) {
			nodes.add((Node)o);
		}
		
		// Schedule its first gossip // TODO: Change timings (maybe random or something)
		nodes.stream().forEach( (Node n) -> {
			this.scheduleGossip(0, new Message<>(n.id, n.id, new RoundStart()));
		});		
	}
}
