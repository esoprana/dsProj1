package dsProj1.msg;

// Support libraries
import org.eclipse.jdt.annotation.NonNull;

// Standard libraries
import java.util.UUID;

public class Message<T extends Object> {
	public final @NonNull UUID source;
	public final @NonNull UUID destination;
	
	public final @NonNull T data;
	
	public Message(@NonNull UUID source, @NonNull UUID destination, @NonNull T data) {
		this.source = source;
		this.destination = destination;
		this.data = data;
	}
}
