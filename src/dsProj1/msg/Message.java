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
	
	public Message(@NonNull Message<T> m) {
		this.source = m.source;
		this.destination = m.destination;
		this.data = m.data; // We consider this a copy, check better
		this.messageId = m.messageId;
	}
	
	@Override
	public String toString() {
		return "Message[source=" + this.source + ", destination=" + this.destination +", data=" + this.data +"]";
	}
	
	@Override
	public int compareTo(Message o) {
		return messageId.compareTo(o.messageId);
	}
}
