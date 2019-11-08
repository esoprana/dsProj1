package dsProj1;

// Support libraries
import org.eclipse.jdt.annotation.NonNull;

// Custom libraries
import dsProj1.msg.Message;

public class Timestamped<T extends Message<?>> implements Comparable<Timestamped>{
	public final double timestamp;
	public final @NonNull T message;
	
	public Timestamped(double timestamp, @NonNull T message) {
		this.timestamp = timestamp;
		this.message = message;
	}
	
	@Override
	public int compareTo(Timestamped other) {
	    int cmpTimestamp = Double.compare(this.timestamp, other.timestamp);
	    
	    if(cmpTimestamp != 0)
	    	return cmpTimestamp;
	
	    return this.message.compareTo(other.message);
	}
}
