package dsProj1;

import java.awt.Color;

import dsProj1.msg.Message;
import dsProj1.msg.data.Event;
import dsProj1.msg.data.Gossip;
import dsProj1.msg.data.RetrieveMessage;
import repast.simphony.space.graph.RepastEdge;

public class MessageStyle extends repast.simphony.visualizationOGL2D.DefaultEdgeStyleOGL2D {	
	@Override
	public Color getColor(RepastEdge<?> edge) {	
		if (!(edge instanceof MessageEdge))
			return super.getColor(edge);
		
		MessageEdge e = (MessageEdge) edge;
		
		double hue;
		
		if (e.message.data instanceof Gossip) {
			hue = 240./360.; // Blue hue
		} else if (e.message.data instanceof RetrieveMessage) {
			hue = 25./360.;  // Orange hue
		} else if (e.message.data instanceof Event) {
			hue = 120./360.; // Green hue
		} else {
			System.err.println(e.message.data.getClass());
			return Color.RED; // If there is some problem with the visualization use red
		}
		
		// If the send was successful use a highly saturated and bright color otherwise the opposite
		return edge.getWeight() != 0 ? Color.getHSBColor((float)hue, 1.f, 1.f) : Color.getHSBColor((float)hue, 0.7f, 0.7f);
	}
	
	@Override
	public int getLineWidth(RepastEdge<?> edge) {
		return 1;
	}
}
