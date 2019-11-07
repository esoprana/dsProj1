package dsProj1;

import org.eclipse.jdt.annotation.NonNull;

public class Timestamped<T> {
	public double timestamp;
	
	@NonNull
	public T data;
	
	public Timestamped(double timestamp, @NonNull T data) {
		this.timestamp = timestamp;
		this.data = data;
	}
}
