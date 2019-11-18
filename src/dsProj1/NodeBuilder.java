package dsProj1;

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
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.RandomCartesianAdder;

// Custom libraries
import dsProj1.msg.Message;
import dsProj1.msg.data.RoundStart;


public class NodeBuilder implements ContextBuilder<Object> {

	/* (non-Javadoc)
	 * @see repast.simphony.dataLoader.ContextBuilder
	 * #build(repast.simphony.context.Context)
	 */
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
			
			// Create node with random connections (of size VIEWS_SIZE) and add it to the context
			Node n = new Node(nodes.get(i), 
							  nodes_uuid_copy.subList(0, (int) Math.round(Options.INITIAL_VIEW_PERC * Options.VIEWS_SIZE)), // copy is done on Node's side
							  oracle);
			context.add(n);
		}
		
		oracle.init(context);

		NetworkBuilder viewBuilder = new NetworkBuilder("view", context, true);
		viewBuilder.buildNetwork();
		
		NetworkBuilder messageBuilder = new NetworkBuilder("message", context, true);
		messageBuilder.buildNetwork();
	    
		// TODO: Auto-generated method stub
		return context;
	}
}
