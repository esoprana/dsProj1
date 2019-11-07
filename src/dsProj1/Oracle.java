package dsProj1;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import dsProj1.msg.DummyStartGossip;
import dsProj1.msg.GenericMessage;
import dsProj1.msg.Gossip;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.Network;
import repast.simphony.util.ContextUtils;

public class Oracle {
	public double currentSeconds = 0;
	
	List<Timestamped<GenericMessage>> messages = new ArrayList<Timestamped<GenericMessage>>(50);
	
	public void send(GenericMessage msg) {
		double delayTo = RandomHelper.createNormal(Options.MEAN_LATENCY, Options.VAR_LATENCY)
				                     .nextDouble(Options.MEAN_LATENCY, Double.POSITIVE_INFINITY);
		messages.add(new Timestamped<GenericMessage>(currentSeconds+1/*delayTo*/, msg));
		//messages.sort(Comparator.comparing((Timestamped<GenericMessage> t) -> t.timestamp));
	}
	
	public void scheduleGossip(double in, DummyStartGossip dg) {
		messages.add(new Timestamped<GenericMessage>(currentSeconds+in, dg));
		//messages.sort(Comparator.comparing((Timestamped<GenericMessage> t) -> t.timestamp));
	}
	
	public Node getNode(UUID id) {
		Context context = ContextUtils.getContext(this);
		for(Object o : context.getObjects(Node.class)) {
			Node n = (Node) o;
			
			if (n.id.equals(id))
				return (Node)o;
		}

		return null;
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void step() throws Exception{
		System.out.println(messages);
		Timestamped<GenericMessage> gm = messages.remove(0);
		this.currentSeconds = gm.timestamp;
		System.out.println("CURRENT TIME: " + this.currentSeconds);
		
		Node destination = this.getNode(gm.data.dest);		
		
		if (destination == null) {
			System.err.println("Destination is null!");
			return; // If no modification happens no reason for updating anything
		}
		
		System.out.println(gm.data.getClass().getName().toUpperCase() + " --- of node " + destination.id);
		System.out.println(gm.data);
		if (gm.data instanceof DummyStartGossip) {
			destination.emitGossip();
		} else if (gm.data instanceof Gossip) {
			destination.handle_gossip((Gossip)gm.data);
			System.out.println(destination.view);
		}
		
		this.updateView();
	}
	
	public void updateView() {
		// Update view network
		Context context = ContextUtils.getContext(this);
		Network views = (Network) context.getProjection("views");

		views.removeEdges();

		HashMap<UUID, Node> nodes = new HashMap<UUID, Node>();

		context.getObjects(Node.class).forEach((Object o) -> {
			Node n = (Node) o;
			nodes.put(n.id, n);
		});

		nodes.values().stream().forEach((Node n) -> {
			n.view.forEach( (UUID l) -> views.addEdge(n, nodes.get(l)) );
		});
	}
}
