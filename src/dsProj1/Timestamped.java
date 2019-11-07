package dsProj1;

// Support libraries
import org.eclipse.jdt.annotation.NonNull;

// Custom libraries
import newMsg.Message;

public class Timestamped<T extends Message<?>> {
	public final double timestamp;
	public final @NonNull T message;
	
	public Timestamped(double timestamp, @NonNull T message) {
		this.timestamp = timestamp;
		this.message = message;
	}
}
