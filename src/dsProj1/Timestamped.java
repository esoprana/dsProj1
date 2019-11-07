package dsProj1;

public class Timestamped<T> {
	public double timestamp;
	public T data;
	
	public Timestamped(double timestamp, T data) {
		this.timestamp = timestamp;
		this.data = data;
	}
}
