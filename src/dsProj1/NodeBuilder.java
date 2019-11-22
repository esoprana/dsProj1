package dsProj1;

import java.io.IOException;
// Standard libraries
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

// Repast libraries
import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunListener;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.util.ContextUtils;
// Custom libraries
import dsProj1.msg.Message;
import dsProj1.msg.data.RoundStart;


public class NodeBuilder implements ContextBuilder<Object> {

	@Override
	public Context<Object> build(Context<Object> context) {
		// Set id of context
		context.setId("dsProj1");
		
		// Load all settings
		Options.load();
		
		// Create grid and space
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace("space", 
																		   context, 
																		   new RandomCartesianAdder<Object>(), 
																		   new repast.simphony.space.continuous.WrapAroundBorders(), 
																		   50, 
																		   50);

		// Create oracle and add it to the context (so that the oracle can obtain the data)
		Oracle oracle = new Oracle();
		context.add(oracle);
		
		int nodeCount = Options.NODE_COUNT;
		
		List<UUID> nodes = new ArrayList<UUID>(nodeCount);
		for (int i = 0; i< nodeCount; i++) {
			nodes.add(UUID.randomUUID());
		}
		
		ArrayList<UUID> nodes_uuid_copy = new ArrayList<UUID>(nodes);
		
		for (int i = 0; i< nodeCount; i++) {
			// Randomize position of nodes ids in nodes_uuid_copy
			Collections.shuffle(nodes_uuid_copy);
			
			UUID currentNode = nodes.get(i);
			
			List<UUID> tmp = new ArrayList<UUID>(nodes_uuid_copy.subList(0, (int) Math.round(Options.INITIAL_VIEW_PERC * Options.VIEWS_SIZE+1)));
			if (tmp.contains(currentNode)) {
				tmp.remove(currentNode);
			} else {
				tmp.remove(tmp.size()-1);
			}

			// Create node with random connections (of size VIEWS_SIZE) and add it to the context
			Node n = new Node(currentNode, tmp);  // copy is done on Node's side
			context.add(n);
			context.add(n.ns);
		}
		
		oracle.init(context);

		NetworkBuilder viewBuilder = new NetworkBuilder("view", context, true);
		viewBuilder.buildNetwork();
		
		NetworkBuilder messageBuilder = new NetworkBuilder("message", context, true);
		messageBuilder.buildNetwork();
	    
		RunEnvironment.getInstance().addRunListener(new RunListener() {
			@Override
			public void stopped() {
				context.getObjects(NodeStat.class).forEach(ns -> ((NodeStat)ns).writeAll());
				NodeStat.writeDead(context);
				
				/*
				try {
					oracle.getStat();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
			}

			@Override
			public void paused() {}

			@Override
			public void started() {}

			@Override
			public void restarted() {}
		});
		
		return context;
	}
}
