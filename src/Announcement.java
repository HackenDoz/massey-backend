import java.io.Serializable;

public class Announcement implements Serializable {
	public int id;
	public String title;
	public String message;
	public long time;

	public Announcement(int id, String title, String message, long time){
		this.id = id;
		this.title = title;
		this.message = message;
		this.time = time;
	}
}
