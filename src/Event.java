import java.io.Serializable;

public class Event implements Serializable{
	public final int id;

	public long startTime;
	public long endTime;
	public String name;
	public String description;

	public Event(int id, long startTime, long endTime, String name, String description) {
		this.id = id;
		this.startTime = startTime;
		this.endTime = endTime;
		this.name = name;
		this.description = description;
	}
}
