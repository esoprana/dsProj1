package dsProj1;

import org.eclipse.jdt.annotation.NonNull;

import bsh.This;

public class Frequency<T> {
	public final @NonNull T data;
	public long frequency = 0;
	
	public Frequency(@NonNull T d) {
		this.data = d;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Frequency)) {
			return false;
		}
		
		Frequency f = (Frequency) obj;

		return this.data.equals(f.data);
	}
}
