package dsProj1;

import dsProj1.msg.Message;
import repast.simphony.space.graph.RepastEdge;

public class MessageEdge extends RepastEdge<Node> {
	public final Message message;
	
	public MessageEdge(Node source, Node destination, Message msg, double width) {
		super(source, destination, true, width);
		
		this.message = msg;
	}
}
