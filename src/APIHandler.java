import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import json.JSONArray;
import json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.SecureRandom;

public class APIHandler implements HttpHandler {

	public transient final Server server;

	public APIHandler(Server server) {
		this.server = server;
	}

	public void handle(HttpExchange t) throws IOException {

		OutputStream output = t.getResponseBody();
		t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
		t.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		t.getResponseHeaders().add("Access-Control-Allow-Headers", "Access-Control-Allow-Headers, Origin,Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");

		String requestT = t.getRequestURI().toString();
		requestT = requestT.substring(5);

		System.out.println("Request: " + requestT);

		if (t.getRequestMethod().equals("OPTIONS")) {
			t.sendResponseHeaders(204, 0);
			t.close();
			return;
		}
		if (!t.getRequestMethod().equals("POST")) {
			String e405 = "<html><head><title>405 Not Allowed!!!</title></head><body>405 method not allowed</body></html>";
			t.sendResponseHeaders(405, e405.length());
			output.write(e405.getBytes());
		} else {
			InputStream input = t.getRequestBody();
			ByteArrayOutputStream o = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			int b;
			while ((b = input.read(buffer)) != -1) {
				o.write(buffer, 0, b);
			}

			try {
				JSONObject object = new JSONObject(o.toString("UTF-8"));
				JSONObject res = null;

				System.out.println("Request body: " + object);

				if (requestT.equals("login")) {
					res = login(object);
				} else if (requestT.equals("register")) {
					res = register(object);
				} else if (requestT.equals("validate_session")) {
					res = verify_key(object);
				} else if (requestT.equals("logout")) {
					res = logout(object);
				} else if (requestT.equals("hackathon/join")) {
					res = hackathon_join(object);
				} else if (requestT.equals("hackathon/create")) {
					res = create_hackathon(object);
				} else if (requestT.equals("hackathon/events/create")) {
					res = create_event(object);
				} else if (requestT.equals("hackathon/events/list")) {
					res = list_events(object);
				} else if (requestT.equals("hackathon/announcements/list")){
					res = list_announcements(object);
				} else if (requestT.equals("hackathon/announcements/create")) {
					res = create_announcement(object);
				} else if (requestT.equals("hackathons/all")) {
					res = list_hackathons();
				} else if (requestT.equals("hackathons")) {
					res = list_joined_hackathons(object);
				} else if (requestT.startsWith("hackathon/")) {
					try {
						int hackathonID = Integer.parseInt(requestT.split("/")[1]);
						object.put("id", hackathonID);

						res = hackathon_id(object);
					} catch (Exception ex) {

					}
				}

				if (res == null) {
					String e404 = "<html><head><title>404 Not Found!!!</title></head><body>404 not found</body></html>";

					System.out.println("Response Error: 404");

					t.sendResponseHeaders(404, e404.length());
					output.write(e404.getBytes("UTF-8"));
				} else {
					System.out.println("Response: " + res.toString());
					byte[] response = res.toString().getBytes("UTF-8");

					t.sendResponseHeaders(200, response.length);
					output.write(response);
				}
			} catch (Exception e) {
				String e400 = "<html><head><title>400 Invalid Request!!!</title></head><body>400 invalid request</body></html>";

				System.out.println("Response Error: 400");

				t.sendResponseHeaders(400, e400.length());
				output.write(e400.getBytes("UTF-8"));
			}
		}

		output.close();

	}

	public JSONObject login(JSONObject data) {
		SecureRandom random = new SecureRandom();

		String username = data.getString("username");
		String password = data.getString("password");
		String deviceID = data.getString("device_id");
		User user = server.users.get(username);
		if (user == null) {
			return new JSONObject("{\"code\":1, \"message\":\"Username/Password Incorrect\"}");
		}
		if (!user.verifyPassword(password.getBytes(Charset.forName("UTF-8")))) {
			return new JSONObject("{\"code\":1, \"message\":\"Username/Password Incorrect\"}");
		}

		int id;
		do {
			id = random.nextInt() & 0x7FFFFFFF;
		} while (server.session.containsKey(id));

		server.session.put(id, new Session(id, deviceID, user));

		return new JSONObject("{\"code\":0, \"id\":" + id + "}");
	}

	public JSONObject register(JSONObject data) {
		JSONObject response = new JSONObject();
		String username = data.getString("username");
		String name = data.getString("name");
		String password = data.getString("password");
		String email = data.getString("email");

		if (server.users.get(username) == null) {
			int userID = server.users.size();
			server.users.put(username, new User(userID, username, name, email));
			server.users.get(username).setPassword(password.getBytes());

			response.put("code", 0);
			response.put("message", "Registration successful");
		} else {
			response.put("code", 1);
			response.put("message", "Username already taken");
		}

		return response;
	}

	public JSONObject logout(JSONObject data) {
		int sessionID = data.getInt("session_id");
		server.session.remove(sessionID);
		return new JSONObject().put("code", 0);
	}

	public JSONObject hackathon_join(JSONObject data) {
		int sessionID = data.getInt("session_id");
		int hackID = data.getInt("hackathon_id");

		JSONObject response = new JSONObject();

		if (server.session.containsKey(sessionID)) {
			Session session = server.session.get(sessionID);
			if (server.hackathons.containsKey(hackID)) {
				session.user.joinedHackathons.put(hackID, server.hackathons.get(hackID));
				server.hackathons.get(hackID).users.put(session.user.username, session.user);

				response.put("code", 0);
				response.put("message", "User successfully joined hackathon");
			} else {
				response.put("code", 1);
				response.put("message", "Hackathon does not exist");
			}
		} else {
			response.put("code", 1);
			response.put("message", "Invalid session key.");
		}
		return response;
	}


	public JSONObject hackathon_id(JSONObject data) {
		int id = data.getInt("id");
		int sessionID = data.getInt("session_id");
		if (server.hackathons.containsKey(id)) {
			Hackathon h = server.hackathons.get(id);
			JSONObject array = new JSONObject();
			array.put("name", h.name);
			array.put("description", h.description);
			array.put("id", h.id);
			array.put("start", h.startTime);
			array.put("end", h.endTime);

			if (server.hackathons.get(id).administrators.containsKey(
					server.session.get(sessionID).user.username
			)){
				array.put("admin", true);
			} else {
				array.put("admin", false);
			}

			JSONArray events = new JSONArray();
			for (Event e : h.events.values()) {
				JSONObject obj = new JSONObject();
				obj.put("id", e.id);
				obj.put("name", e.name);
				obj.put("description", e.description);
				obj.put("start", e.startTime);
				obj.put("end", e.endTime);
				events.put(obj);
			}

			array.put("events", events);

			return array;

		} else {
			return new JSONObject("{\"code\':1, \"message\":\"Hackathon does not exist\"}");
		}
	}

	public JSONObject hackathons(JSONObject data) {

		String filter = "";
		if (data.has("filter"))
			filter = data.getString("filter");

		JSONArray array = new JSONArray();

		for (Hackathon hack : server.hackathons.values()) {
			if (hack.name.toLowerCase().contains(filter)) {
				JSONObject obj = new JSONObject();
				obj.put("id", hack.id);
				obj.put("name", hack.name);
				obj.put("start", hack.startTime);
				obj.put("end", hack.endTime);
				array.put(obj);
			}
		}

		JSONObject obj = new JSONObject();
		obj.put("code", 0);
		obj.put("hackathons", array);

		return obj;
	}

	public JSONObject create_hackathon(JSONObject data) {
		String name = data.getString("hackathon_name");
		String description = data.getString("hackathon_description");
		String location = data.getString("location");
		int sessionID = data.getInt("session_id");
		long startTime = data.getLong("start_time");
		long endTime = data.getLong("end_time");

		JSONObject response = new JSONObject();

		if (server.session.containsKey(sessionID)) {
			if (name != null && description != null && startTime != 0 && endTime != 0) {
				int hackathonID = server.hackathons.size();
				server.hackathons.put(hackathonID,
						new Hackathon(hackathonID, name, location, startTime, endTime));

				server.hackathons.get(hackathonID).owner = server.session.get(sessionID).user.username;
				server.hackathons.get(hackathonID).administrators.put(
						server.session.get(sessionID).user.username,
						server.users.get(server.session.get(sessionID).user.username)
				);

				response.put("code", 0);
				response.put("message", "Hackathon successfully created.");
			} else {
				response.put("code", 1);
				response.put("message", "Insufficient/Invalid data provided.");
			}
		} else {
			response.put("code", 1);
			response.put("message", "Session ID not present. ");
		}

		return response;
	}

	public JSONObject create_event(JSONObject data) {
		int hackathon_id = data.getInt("hackathon_id");
		int sessionID = data.getInt("session_id");
		String name = data.getString("event_name");
		String description = data.getString("event_description");
		long startTime = data.getLong("start_time");
		long endTime = data.getLong("end_time");

		JSONObject response = new JSONObject();

		if (server.hackathons.containsKey(hackathon_id)) {
			int eventID = server.hackathons.get(hackathon_id).events.size();

			if (server.session.containsKey(sessionID)) {
				String username = server.session.get(sessionID).user.username;
				if (server.hackathons.get(hackathon_id).administrators.containsKey(username)) {
					server.hackathons.get(hackathon_id).events.put(
							eventID,
							new Event(eventID, startTime, endTime, name, description)
					);

					response.put("code", 0);
					response.put("message", "Event added successfully.");
				} else {
					response.put("code", 1);
					response.put("message", "User not authorized.");
				}
			} else {
				response.put("code", 1);
				response.put("message", "Invalid session key.");
			}
		} else {
			response.put("code", 1);
			response.put("message", "Hackathon not found.");
		}

		return response;
	}

	public JSONObject list_events(JSONObject data) {
		int hackathon_id = data.getInt("id");
		JSONArray response = new JSONArray();

		for (Event ev : server.hackathons.get(hackathon_id).events.values()) {
			JSONObject obj = new JSONObject();
			obj.put("name", ev.name);
			obj.put("description", ev.description);
			obj.put("start_time", ev.startTime);
			obj.put("end_time", ev.endTime);

			response.put(obj);
		}

		JSONObject res = new JSONObject();
		res.put("events", response);

		return res;
	}

//	public JSONObject create_announcement(JSONObject data) {
//		if(!data.has("session_id")) {
//			return new JSONObject().put("code", 1).put("message", "No session id");
//		}
//
//		int session = data.getInt("session_id");
//		User user = server.users.get(session);
//		if(user == null) {
//			return new JSONObject().put("code", 1).put("message", "Invalid session id");
//		}
//
//
//
//		//HttpURLConnection connection = new HttpURLConnection();
//
//
//	}

	public JSONObject list_hackathons() {
		JSONArray hacks = new JSONArray();

		for (Hackathon hh : server.hackathons.values()) {
			JSONObject obj = new JSONObject();

			obj.put("id", hh.id);
			obj.put("name", hh.name);
			obj.put("description", hh.description);
			obj.put("start_time", hh.startTime);
			obj.put("end_time", hh.endTime);
			obj.put("location", hh.location);

			hacks.put(obj);
		}

		JSONObject response = new JSONObject();
		response.put("hackathons", hacks);

		return response;
	}

	public JSONObject list_joined_hackathons(JSONObject data) {
		int sessionID = data.getInt("session_id");

		JSONObject response = new JSONObject();

		if (server.session.containsKey(sessionID)) {
			JSONArray ar = new JSONArray();

			for (Hackathon hh : server.hackathons.values()) {
				JSONObject obj = new JSONObject();
				obj.put("id", hh.id);
				obj.put("name", hh.name);
				obj.put("description", hh.description);
				obj.put("location", hh.location);
				obj.put("start_time", hh.startTime);
				obj.put("end_time", hh.endTime);

				ar.put(obj);
			}

			response.put("hackathons", ar);
		} else {
			response.put("code", 1);
			response.put("message", "Invalid session ID");
		}

		return response;

	}

	public JSONObject verify_key(JSONObject data) {
		int sessionID = data.getInt("session_id");
		JSONObject response = new JSONObject();

		if (server.session.containsKey(sessionID)) {
			response.put("code", 0);
		} else {
			response.put("code", 1);
		}

		return response;
	}

	public JSONObject list_announcements(JSONObject data){
		int hackathonID = data.getInt("hackathon_id");

		JSONObject response = new JSONObject();
		if (server.hackathons.containsKey(hackathonID)){
			JSONArray items = new JSONArray();

			for (Announcement an : server.hackathons.get(hackathonID).announcements.values()){
				JSONObject obj = new JSONObject();
				obj.put("title", an.title);
				obj.put("message", an.message);
				obj.put("time", an.time);

				items.put(obj);
			}

			response.put("announcements", items);
		} else {
			response.put("code", 1);
			response.put("message", "Invalid hackathon ID.");
		}

		return response;
	}

	public JSONObject create_announcement(JSONObject data){
		int sessionID = data.getInt("session_id");
		String title = data.getString("title");
		String message = data.getString("message");
		//long time = data.getLong("time");
		int hackathonID = data.getInt("hackathon_id");

		JSONObject response = new JSONObject();

		if (server.session.containsKey(sessionID)){
			if (server.hackathons.get(hackathonID).administrators.containsKey(server.session.get(sessionID).user.username)){
				int id = server.hackathons.get(hackathonID).announcements.size();
				server.hackathons.get(hackathonID).announcements.put(
					id, new Announcement(id, title, message, System.currentTimeMillis())
				);

				PushNotificationSender sender = new PushNotificationSender(server.hackathons.get(hackathonID), server);
				sender.sendAnnouncement(server.hackathons.get(hackathonID).announcements.get(id));

				response.put("code", 0);
				response.put("message", "Announcement successfully created. ");

			} else {
				response.put("code", 1);
				response.put("message", "User not authorized.");
			}
		} else {
			response.put("code", 1);
			response.put("message", "Invalid session ID");
		}

		return response;
	}
}
