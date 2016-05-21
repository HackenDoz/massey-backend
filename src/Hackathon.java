import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

public class Hackathon implements Serializable {

    public final int id;
	public String name;
	public String description;
    public long startTime;
    public long endTime;

	public final HashMap<Integer, User> users = new HashMap<>();
	public final HashMap<Integer, Event> events = new HashMap<>();

	public Hackathon(int id, String name, long startTime, long endTime) {
		this.id = id;
		this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
		this.description = "";
	}


}
