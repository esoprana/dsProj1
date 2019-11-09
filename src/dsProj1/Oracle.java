package dsProj1;

// Support libraries
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

// Standard libraries
import java.util.HashMap;
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
import dsProj1.msg.data.Gossip;

public class Oracle {
	public double currentSeconds = 0;
	
	public static double normal(double mean, double var) {
		return RandomHelper.createNormal(mean, var)
		 				   .apply(RandomHelper.nextDoubleFromTo(0, 1));
	}
	
	public static double normalCut(double mean, double var) {
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
		
		{
			int d = (int) (msg.timestamp/(3600*24));
			int h = (int) ((msg.timestamp % (3600*24)) / 3600);
			int m = (int) ((msg.timestamp % 3600) / 60);
			int s = (int) (msg.timestamp % 60);
			int ms = (int) ((msg.timestamp % 1)*1000);
			double ns = ((msg.timestamp % 1)*1000) % 1;

			System.out.printf("%dd%dh%dm%ds%dms%fns - %s %s\n", d, h, m, s, ms, ns, 
														  	  	msg.message.data.getClass().getSimpleName(), 
														  	  	msg.message);
		}
		
		// If destination is dead, update the view and exit immediately (do not use any handle*)
		if (!destination.alive) {
			this.updateView(msg);
			return;
		}
		
		if (msg.message.data instanceof RoundStart) {
			destination.startRound();
		} else if (msg.message.data instanceof Gossip) {
			destination.handleGossip( (Message<Gossip>) msg.message);
		} else if (msg.message.data instanceof Event) {
			destination.handleEvent( (Event) msg.message.data);
		} else {
			throw new Exception("Unrecognized type of message!");
		}
		
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
	
		Network networkView = (Network) context.getProjection("view");
		Network networkMessage = (Network) context.getProjection("message");

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
}
