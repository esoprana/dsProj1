package dsProj1.msg.data;

// Support libraries
import org.eclipse.jdt.annotation.NonNull;

public class ExternalData<T> {
	public final @NonNull T data;
	
	public ExternalData(@NonNull T data) {
		this.data = data;
	}
	
	@Override
	public String toString() {
		return "ExternalData[data=" + this.data + "]";
	}
}
