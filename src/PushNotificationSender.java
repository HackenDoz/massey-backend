import json.JSONArray;
import json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

public class PushNotificationSender {
	public Hackathon hackathon;
	public transient final Server server;
	public static final String authKey = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJiMDgyOWQ5Yy0zNTAwLTQ5OTItYmIxNC00NWNlYjFjMjQxODQifQ.nJNYBVFQEqiPZe8Wh842ZEDbuK9EBblY-g1_tCaqzK4";

	public PushNotificationSender(Hackathon hackathon, Server server){
		this.hackathon = hackathon;
		this.server = server;
	}

	public void sendRequest(JSONObject obj){
		try {

			HttpURLConnection connection = (HttpURLConnection)new URL("https://api.ionic.io/push/notifications").openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Authorization", "Bearer " + authKey);
			connection.setRequestProperty("Content-type", "application/json");
			connection.setDoOutput(true);

			byte[] data = obj.toString().getBytes(Charset.forName("UTF-8"));

			connection.setFixedLengthStreamingMode(data.length);
			connection.getOutputStream().write(data);

			InputStream input = connection.getInputStream();
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			int l;
			while((l = input.read(buffer)) != -1) {
				output.write(buffer, 0, l);
			}

			if(connection.getResponseCode() != 201) {
				throw new RuntimeException("Failed, received " + connection.getResponseCode());
			}

			System.out.println(new String(output.toByteArray()));

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void sendAnnouncement(Announcement ass){
		JSONObject obj = new JSONObject();

		JSONArray tokens = new JSONArray();
		for (Session ss : server.session.values()){
			if (hackathon.users.containsKey(ss.user.username)){
				tokens.put(ss.deviceID);
			}
		}

		obj.put("tokens", tokens);
		obj.put("profile", "fake_push_profile");

        JSONObject notification = new JSONObject();
		notification.put("title", String.format("%s: %s", hackathon.name, ass.title));
		notification.put("message", ass.message);

        obj.put("notification", notification);

		sendRequest(obj);
	}

}
