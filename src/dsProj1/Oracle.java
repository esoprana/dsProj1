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
import newMsg.Message;


public class Oracle {
	public double currentSeconds = 0;
	
	@NonNull List<@NonNull Timestamped<Message<?>>> messages = new ArrayList<@NonNull Timestamped<Message<?>>>(50);
	
	public void send(@NonNull Message<?> msg) {
		double delayTo = RandomHelper.createNormal(Options.MEAN_LATENCY, Options.VAR_LATENCY)
				                     .nextDouble(Options.MEAN_LATENCY, Double.POSITIVE_INFINITY);
		messages.add(new Timestamped<Message<?>>(currentSeconds+1/*delayTo*/, msg));
		//messages.sort(Comparator.comparing((Timestamped t) -> t.timestamp));
	}
	
	public void scheduleGossip(double in, @NonNull Message<newMsg.data.DummyStartGossip> dg) {
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
	public void step() {
		if (messages.isEmpty()) {
			System.err.println("No more messages!");
			return;
		}
		
		Timestamped<Message<?>> gm = messages.remove(0);
		this.currentSeconds = gm.timestamp;
		
		Node destination = this.getNode(gm.message.destination);		
		
		if (destination == null) {
			System.err.println("Destination is null!");
			return; // If no modification happens no reason for updating anything
		}
		
		System.out.println(gm.message.getClass().getName().toUpperCase() + " --- of node " + destination.id);
		System.out.println(gm.message);
		
		if (gm.message.data instanceof newMsg.data.DummyStartGossip) {
			destination.emitGossip();
		} else if (gm.message.data instanceof newMsg.data.Gossip) {
			destination.handle_gossip((Message<newMsg.data.Gossip>)gm.message);
			System.out.println(destination.view);
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
				 from.view.stream()
				     .map(nodes::get)
					 .forEach( (Node to) -> views.addEdge(from, to) );
			  });
	}
}
