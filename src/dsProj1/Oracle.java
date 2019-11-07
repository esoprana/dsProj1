package dsProj1;

// Support libraries
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

// Standard libraries
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	
	@NonNull List<@NonNull Timestamped<Message<?>>> messages = new ArrayList<@NonNull Timestamped<Message<?>>>(50);
	
	public void send(@NonNull Message<?> msg) {
		double delayTo = RandomHelper.createNormal(Options.MEAN_LATENCY, Options.VAR_LATENCY)
				                     .nextDouble(Options.MEAN_LATENCY, Double.POSITIVE_INFINITY);
		messages.add(new Timestamped<Message<?>>(currentSeconds+1/*delayTo*/, msg));
		//messages.sort(Comparator.comparing((Timestamped t) -> t.timestamp));
	}
	
	public void scheduleGossip(double in, @NonNull Message<DummyStartGossip> dg) {
		messages.add(new Timestamped<Message<?>>(currentSeconds+in, dg));
		//messages.sort(Comparator.comparing((Timestamped t) -> t.timestamp));
	}
	
	public @Nullable Node getNode(@NonNull UUID id) {
		Context context = ContextUtils.getContext(this);
		for(Object o : context.getObjects(Node.class)) {
			Node n = (Node) o;
			
			if (n.id.equals(id))
				return (Node)o;
		}

		return null;
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void step() throws Exception {
		// If nothing is scheduled something bad happened (there should be at least some periodic event)
		if (messages.isEmpty())
			throw new Exception("No more messages/events!");
		
		Timestamped<Message<?>> gm = messages.remove(0);
		this.currentSeconds = gm.timestamp;
		
		Node destination = this.getNode(gm.message.destination);		
		
		// If there is no destination something bad happened (the node should never be removed from the context, use DEAD status instead)
		if (destination == null) {
			throw new Exception("Destination of message is null!");
		}
		
		System.out.println(gm.message.getClass().getName().toUpperCase() + " --- of node " + destination.id + "(" + (destination.alive?"alive":"dead") + ")");
		System.out.println(gm.message);

		// If destination is dead, update the view and exit immediately (do not use any handle*)
		if (!destination.alive) {
			this.updateView();
			return;
		}
		
		if (gm.message.data instanceof DummyStartGossip) {
			destination.emitGossip();
		} else if (gm.message.data instanceof Gossip) {
			destination.handle_gossip((Message<Gossip>)gm.message);
		} else if (gm.message.data instanceof Event) {
			destination.handleEvent( (Event) gm.message.data);
		} else {
			throw new Exception("Unrecognized type of message!");
		}
		
		this.updateView();
	}
	
	public void updateView() {
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

		// For each node... 
		nodes.values()
			 .stream()
			 .forEach( (Node from) -> {
				 // ... get it's view, find the corresponding nodes, and add a link to the network
				 from.view
				 	 .stream()
				     .map(nodes::get)
					 .forEach( (Node to) -> views.addEdge(from, to) );
			  });
	}
}
