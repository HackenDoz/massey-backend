public class Session {

	public int id;
	public String deviceID;
	public User user;

	public Session(int id, String deviceID, User user) {
		this.id = id;
		this.deviceID = deviceID;
		this.user = user;
	}
}
