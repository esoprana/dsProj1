package dsProj1;

// Support libraries
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

// Standard libraries
import java.util.ArrayList;
import java.util.Comparator;
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
import dsProj1.msg.data.DummyStartGossip;
import dsProj1.msg.data.Event;
import dsProj1.msg.data.Gossip;

public class Oracle {
	public double currentSeconds = 0;
	
	@NonNull TreeSet<@NonNull Timestamped<Message<?>>> messages = new TreeSet<@NonNull Timestamped<Message<?>>>();
	
	public void send(@NonNull Message<?> msg) {
		double delayTo = RandomHelper.createNormal(Options.MEAN_LATENCY, Options.VAR_LATENCY)
				                     .nextDouble(Options.MEAN_LATENCY, Double.POSITIVE_INFINITY);
		messages.add(new Timestamped<Message<?>>(currentSeconds+delayTo, msg));
	}
	
	public void scheduleGossip(double in, @NonNull Message<DummyStartGossip> dg) {
		messages.add(new Timestamped<Message<?>>(currentSeconds+in, dg));
	}
	
	public @Nullable Node getNode(@NonNull UUID id) {
		Context context = ContextUtils.getContext(this);
		for(Object o : context.getObjects(Node.class)) {
			Node n = (Node) o;
			
			if (n.id.equals(id))
				return n;
		}

		return null;
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void step() throws Exception {
		@Nullable Timestamped<Message<?>> msg = messages.pollFirst();
		
		// If nothing is scheduled something bad happened (there should be at least some periodic event)
		if(msg == null)
			throw new Exception("No more messages/events!");
		 
		this.currentSeconds = msg.timestamp;
		
		Node destination = this.getNode(msg.message.destination);		
		
		// If there is no destination something bad happened (the node should never be removed from the context, use DEAD status instead)
		if (destination == null) {
			throw new Exception("Destination of message is null!");
		}
		
		System.out.println(msg.message.getClass().getName().toUpperCase() + " --- of node " + destination.id + "(" + (destination.alive?"alive":"dead") + ")");
		System.out.println(msg.message);

		// If destination is dead, update the view and exit immediately (do not use any handle*)
		if (!destination.alive) {
			this.updateView(destination, msg);
			return;
		}
		
		if (msg.message.data instanceof DummyStartGossip) {
			destination.emitGossip();
		} else if (msg.message.data instanceof Gossip) {
			destination.handle_gossip( (Message<Gossip>) msg.message);
		} else if (msg.message.data instanceof Event) {
			destination.handleEvent( (Event) msg.message.data);
		} else {
			throw new Exception("Unrecognized type of message!");
		}
		
		this.updateView(destination, msg);
	}
	
	public void updateView(Node currentNode, Timestamped<Message<?>> message) {
		// --- UPDATE VIEW NETWORK
		Context context = ContextUtils.getContext(this);
		Network views = (Network) context.getProjection("views");

		// Remove old edges
		views.removeEdges();

		// Create a map (id: UUID -> node: Node)
		HashMap<UUID, Node> nodes = new HashMap<UUID, Node>();
		context.getObjects(Node.class).forEach((Object o) -> {
			Node n = (Node) o;
			nodes.put(n.id, n);
		});
		
		currentNode.view
	 	     	   .stream()
	 	     	   .map(nodes::get)
	 	     	   .forEach( (Node to) -> views.addEdge(currentNode, to) );
	}
}
