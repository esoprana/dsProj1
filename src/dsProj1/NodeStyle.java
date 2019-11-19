package dsProj1;

import java.awt.Color;
import java.awt.Font;

public class NodeStyle extends repast.simphony.visualizationOGL2D.DefaultStyleOGL2D {
	@Override
	public Color getColor(Object object) {
		if (object instanceof Node) {
	    	Node n = (Node) object;
	    	
	    	return n.alive?Color.BLUE:Color.RED;
	    }
		
		return super.getColor(object);
	}
	
	@Override
	public String getLabel(Object object) {
		if (object instanceof Node) {
	    	Node n = (Node) object;

	    	return null;
	    }
		
		return super.getLabel(object);
	}
	
	@Override
	public Font getLabelFont(Object object) {
		return new Font(Font.SERIF, Font.ITALIC, 9);
	}
}
