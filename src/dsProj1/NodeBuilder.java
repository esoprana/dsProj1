package dsProj1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import dsProj1.msg.DummyStartGossip;
import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;

import repast.simphony.dataLoader.ContextBuilder;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.graph.Network;
import repast.simphony.space.grid.WrapAroundBorders;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;

public class NodeBuilder implements ContextBuilder<Object> {

	/* (non-Javadoc)
	 * @see repast.simphony.dataLoader.ContextBuilder
	 * #build(repast.simphony.context.Context)
	 */
	@Override
	public Context build(Context<Object> context) {
		context.setId("dsProj1");
		
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space = spaceFactory.createContinuousSpace("space", 
																		   context, 
																		   new RandomCartesianAdder<Object>(), 
																		   new repast.simphony.space.continuous.WrapAroundBorders(), 
																		   50, 
																		   50);
		
		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		
		Grid<Object> grid = gridFactory.createGrid("grid", 
												   context, 
												   new GridBuilderParameters<Object>(new WrapAroundBorders(), 
														                             new SimpleGridAdder<Object>(), 
														                             true, 
														                             50, 
														                             50));

		Oracle oracle = new Oracle();
		context.add(oracle);
		
		int nodeCount = 35;
		
		List<UUID> nodes = new ArrayList<UUID>(nodeCount);
		for (int i = 0; i< nodeCount; i++) {
			nodes.add(UUID.randomUUID());
		}
		
		ArrayList<UUID> nodes_uuid_copy = new ArrayList<UUID>(nodes);
		
		for (int i = 0; i< nodeCount; i++) {
			// Randomize position of nodes ids in nodes_uuid_copy
			Collections.shuffle(nodes_uuid_copy);
			
			// Create node with random connections (of size VIEWS_SIZE)
			Node n = new Node(space, 
							  grid, 
							  nodes.get(i), 
							  nodes_uuid_copy.subList(0, Options.VIEWS_SIZE),
							  oracle);
			context.add(n);

			System.out.println("ADDING GOSSIP OF "+ n.id);
			oracle.scheduleGossip(0, new DummyStartGossip(n.id));
		}

		for (Object obj: context) {
			NdPoint pt = space.getLocation(obj);
			grid.moveTo(obj, (int)pt.getX(), (int)pt.getY());
		}


		NetworkBuilder builder = new NetworkBuilder("views", context, false);
		Network network = builder.buildNetwork();
	    
		// TODO: Auto-generated method stub
		return context;
	}
}
