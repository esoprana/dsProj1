package dsProj1.msg;

// Support libraries
import org.eclipse.jdt.annotation.NonNull;

// Standard libraries
import java.util.UUID;

public class Message<T extends Object> implements Comparable<Message> {
	private final @NonNull UUID messageId; // Used to impose strict ordering
	
	public final @NonNull UUID source;
	public final @NonNull UUID destination;
	
	public final @NonNull T data;
	
	public Message(@NonNull UUID source, @NonNull UUID destination, @NonNull T data) {
		this.source = source;
		this.destination = destination;
		this.data = data;
		
		this.messageId = UUID.randomUUID();
	}
	
	@Override
	public String toString() {
		return "{ source: " + this.source + ",\n" +
			   "  destination: " + this.destination + ",\n" + 
			   "  data: " + this.data.toString() + " }";
	}
	
	@Override
	public int compareTo(Message o) {
		return messageId.compareTo(o.messageId);
	}
}
