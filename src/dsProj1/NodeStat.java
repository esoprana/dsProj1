package dsProj1;

// Support libraries
import org.eclipse.jdt.annotation.NonNull;

import dsProj1.msg.Message;
import dsProj1.msg.data.Event;
import dsProj1.msg.data.ExternalData;
import dsProj1.msg.data.Gossip;
import dsProj1.msg.data.RetrieveMessage;
import dsProj1.msg.data.RoundStart;
import repast.simphony.context.Context;
import repast.simphony.util.ContextUtils;

// Standard libraries
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

class EventIdStat {
	public final EventId eventId;
	public final double timestamp;
	public final long round;
	
	public EventIdStat(EventId eventId, double timestamp, long round) {
		this.eventId = eventId;
		this.timestamp = timestamp;
		this.round = round;
	}
}

class Stat {
	public final long id;
	public final double timestamp;
	public final long round;
	
	public Stat(long id, double timestamp, long round) {
		this.id = id;
		this.timestamp = timestamp;
		this.round = round;
	}	
}

public class NodeStat {
	private final @NonNull Node ofNode;
	
	private ArrayList<Stat> eventReceived = new ArrayList<>();
	private ArrayList<Stat> eventHandled = new ArrayList<>();
	private ArrayList<Stat> createdEvent = new ArrayList<>();
	
	private double deadAtTimestamp = -1;
	
	private ArrayList<EventIdStat> retrieveTries = new ArrayList<>();
	private ArrayList<EventIdStat> retrieveFail = new ArrayList<>();
	private ArrayList<EventIdStat> retrieveFound = new ArrayList<>();
	
	public NodeStat(@NonNull Node node) {
		this.ofNode = node;
	}
	
	private Oracle getOracle() {
		return (Oracle)ContextUtils.getContext(this).getObjects(Oracle.class).get(0);
	}
	
	public UUID getId() {
		return this.ofNode.id;
	}
	
	public long getCurrentRound() {
		return this.ofNode.currentRound;
	}
	
	public double getCurrentTimestamp() {
		return this.getOracle().currentSeconds;
	}
	
	public Node getNode() {
		return this.ofNode;
	}
	
	public long getNoReceptionsForId(long id) {
		return this.eventReceived.stream().filter(s -> s.id == id).count();
	}
	
	public long getNoReceptionsAtRound(long round) {
		return this.eventReceived.stream().filter(s -> s.round == round).count();
	}
	
	public long getNoReceptionForIdAtRound(long id, long round) {
		return this.eventReceived.stream().filter(s -> s.round == round && s.id == id).count();
	}
	
	public long getNoHandledForId(long id) {
		return this.eventHandled.stream().filter(s -> s.id == id).count();
	}
	
	public long getNoHandledAtRound(long round) {
		return this.eventHandled.stream().filter(s -> s.round == round).count();
	}
	
	public long getNoHandledForIdAtRound(long id, long round) {
		return this.eventHandled.stream().filter(s -> s.id == id && s.round == round).count();
	}
	
	public long getNoEventAtRound(long round) {
		return this.createdEvent.stream().filter(s -> s.round == round).count();
	}
	
	public void die(double currentTimestamp) {
		this.deadAtTimestamp = this.getCurrentTimestamp();
		this.ofNode.alive = false;
	}
	
	public void handleMessage(@NonNull Message msg) throws Exception {
		long round = this.getCurrentRound();
		double timestamp = this.getCurrentTimestamp();

		if (msg.data instanceof Gossip) {			
			Gossip g = (Gossip)msg.data;
			g.events.stream()
					.map(e -> (Long)e.data)
					.map( id -> new Stat(id, timestamp, round))
					.forEach(this.eventReceived::add);
		}
		
		if (msg.data instanceof Event) {
			Event e = (Event)msg.data;
			this.eventReceived.add(new Stat((Long)e.data, timestamp, round));
			this.retrieveFound.add(new EventIdStat(e.eventId, timestamp, round));
		}
		
		if (msg.data instanceof ExternalData) {
			ExternalData e = (ExternalData)msg.data;
			this.eventReceived.add(new Stat((Long)e.data, timestamp, round));
			this.createdEvent.add(new Stat((Long)e.data, timestamp, round));
		}
		
		this.ofNode.handleMessage(msg);
	}
	
	public void send(@NonNull Message msg) {
		if (msg.data instanceof RetrieveMessage) {
			long round = this.getCurrentRound();
			double timestamp = this.getCurrentTimestamp();

			RetrieveMessage rm = (RetrieveMessage) msg.data; 
			this.retrieveTries.add(new EventIdStat(rm.eventIdRequested, timestamp, round));
		}
			
		this.getOracle().send(msg);
	}
	
	public void scheduleGossip(double delay, @NonNull Message<RoundStart> dg) {
		this.getOracle().scheduleGossip(delay, dg);
	}
	
	public void stat(Event ev) {
		this.eventHandled.add(new Stat((Long)ev.data, this.getCurrentTimestamp(), this.getCurrentRound()));
		//this.getOracle().stat(this.getId(), ev);
	}
	
	public void retrieveFail(EventId eventId) {
		long round = this.getCurrentRound();
		double timestamp = this.getCurrentTimestamp();

		this.retrieveFail.add(new EventIdStat(eventId, timestamp, round));
	}
	
	private static void writeStatArray(String id, ArrayList<Stat> stats, String name) throws IOException {
		name = name.endsWith(java.io.File.separator) ? name : name + java.io.File.separator;
		String path = Options.OUTPUT_FOLDER + name + id + ".csv";

		java.io.File dir = new java.io.File(Options.OUTPUT_FOLDER + name);
		if (!dir.exists())
			dir.mkdirs();
		
		java.io.File file = new java.io.File(path);
		boolean exists = file.exists();
		java.io.FileWriter writer = new java.io.FileWriter(file, true);

		if (!exists)
			writer.write("id, timestamp, round\n");
		
		for (Stat s : stats) {
			writer.write(s.id + ", " + s.timestamp + ", " + s.round + "\n");
		}
		
		stats.clear();
		
		writer.flush();
		writer.close();	
	}
	
	private static void writeEventIdArray(String id, ArrayList<EventIdStat> stats, String name) throws IOException {
		name = name.endsWith(java.io.File.separator) ? name : name + java.io.File.separator;
		String path = Options.OUTPUT_FOLDER + name + id + ".csv";

		java.io.File dir = new java.io.File(Options.OUTPUT_FOLDER + name);
		if (!dir.exists())
			dir.mkdirs();
		
		java.io.File file = new java.io.File(path);
		boolean exists = file.exists();
		java.io.FileWriter writer = new java.io.FileWriter(file, true);

		if (!exists)
			writer.write("source, id, timestamp, round\n");
		
		for (EventIdStat s : stats) {
			writer.write(s.eventId.source + ", " + s.eventId.id + ", " + s.timestamp + ", " + s.round + "\n");
		}
		
		stats.clear();
		
		writer.flush();
		writer.close();	
	}

	
	public void writeEventReceived() {
		try {
			writeStatArray(this.getId().toString(), this.eventReceived, "eventReceived");
			this.eventReceived.clear();
		} catch (IOException e) {
			System.err.println("Failed to write eventReceived for " + this.getId().toString());
			e.printStackTrace();
		}
	}
	
	public void writeEventHandled() {
		try {
			writeStatArray(this.getId().toString(), this.eventHandled, "eventHandled");
			this.eventHandled.clear();
		} catch (IOException e) {
			System.err.println("Failed to write eventHandled for " + this.getId().toString());
			e.printStackTrace();
		}

	}
	
	public void writeCreatedEvent() {
		try {
			writeStatArray(this.getId().toString(), this.createdEvent, "createEvent");
			this.createdEvent.clear();
		} catch (IOException e) {
			System.err.println("Failed to write createEvent for " + this.getId().toString());
			e.printStackTrace();
		}
	}
	
	public void writeRetrieveFail() {
		try {
			writeEventIdArray(this.getId().toString(), this.retrieveFail, "retrieveFail");
			this.retrieveFail.clear();
		} catch (IOException e) {
			System.err.println("Failed to write retrieveFail for " + this.getId().toString());
			e.printStackTrace();
		}
	}

	public void writeRetrieveTries() {
		try {
			writeEventIdArray(this.getId().toString(), this.retrieveTries, "retrieveTries");
			this.retrieveTries.clear();
		} catch (IOException e) {
			System.err.println("Failed to write retrieveTries for " + this.getId().toString());
			e.printStackTrace();
		}
	}

	public void writeRetrieveFound() {
		try {
			writeEventIdArray(this.getId().toString(), this.retrieveFound, "retrieveFound");
			this.retrieveFound.clear();
		} catch (IOException e) {
			System.err.println("Failed to write retrieveFound for " + this.getId().toString());
			e.printStackTrace();
		}
	}

	public void writeAll() {
		this.writeEventReceived();
		this.writeEventHandled();
		this.writeCreatedEvent();
		
		this.writeRetrieveTries();
		this.writeRetrieveFail();
		this.writeRetrieveFound();
	}
	
	public static void writeDead(Context c) {
		try {
			String path = Options.OUTPUT_FOLDER + "dead.csv";

			java.io.File dir = new java.io.File(Options.OUTPUT_FOLDER);
			if (!dir.exists())
				dir.mkdirs();
			
			java.io.FileWriter writer = new java.io.FileWriter(new java.io.File(path), false);

			writer.write("id, timestamp\n");
			for (Object o : c.getObjects(NodeStat.class)) {
				NodeStat ns = (NodeStat) o;
				writer.write(ns.getId() + ", " + ns.deadAtTimestamp + "\n");
			}
			
			writer.flush();
			writer.close();	
		} catch (IOException e) {
			System.err.println("Failed to write dead");
			e.printStackTrace();
		}
	}
}
