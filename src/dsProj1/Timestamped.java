package dsProj1;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

// Support libraries
import org.eclipse.jdt.annotation.NonNull;

// Custom libraries
import dsProj1.msg.Message;

public class Timestamped<T extends Message> implements Comparable<Timestamped>{
	public final double timestamp;
	public final @NonNull T message;
	
	public Timestamped(double timestamp, @NonNull T message) {
		this.timestamp = timestamp;
		this.message = message;
	}
	
	public Timestamped(Timestamped<T> t) {
		this.timestamp = t.timestamp;
		this.message = (@NonNull T) new Message(t.message); // TODO: Why is cast necessary? (java problem)
	}
	
	@Override
	public int compareTo(Timestamped other) {
	    int cmpTimestamp = Double.compare(this.timestamp, other.timestamp);
	    
	    if(cmpTimestamp != 0)
	    	return cmpTimestamp;
	
	    return this.message.compareTo(other.message);
	}
	
	@Override
	public String toString() {
		DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setDecimalSeparator('.');
		
		DecimalFormat fmt = new DecimalFormat("#0.000000", symbols);
		return "Timestamped[timestamp="+ fmt.format(this.timestamp) + ", message=" + this.message + "]";
	}
}
