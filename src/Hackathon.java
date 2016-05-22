import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

public class Hackathon implements Serializable {

    public final int id;
	public String name;
	public String description;
	public String location;
    public long startTime;
    public long endTime;

	public String owner;

	public final HashMap<String, User> users = new HashMap<>();
	public final HashMap<String, User> administrators = new HashMap<>();
	public final HashMap<Integer, Event> events = new HashMap<>();
	public final HashMap<Integer, Announcement> announcements = new HashMap<>();

	public Hackathon(int id, String name, String location, long startTime, long endTime) {
		this.id = id;
		this.name = name;
		this.location = location;
        this.startTime = startTime;
        this.endTime = endTime;
		this.description = "";
	}


}
