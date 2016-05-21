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

		String requestT = t.getRequestURI().toString();
		requestT = requestT.substring(5);

		if(!t.getRequestMethod().equals("POST")) {
			String e405 = "<html><head><title>405 Not Allowed!!!</title></head><body>405 method not allowed</body></html>";
			t.sendResponseHeaders(405, e405.length());
			output.write(e405.getBytes());
		} else {
			InputStream input = t.getRequestBody();
			ByteArrayOutputStream o = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			int b;
			while((b = input.read(buffer)) != -1) {
				o.write(buffer, 0, b);
			}

			try {

				JSONObject object = new JSONObject(o.toString("UTF-8"));

				JSONObject res = null;

				switch(requestT) {
					case "login":
						res = login(object);
						break;
					case "register":
						res = register(object);
						break;
					case "hackathon/join":
						res = hackathon_join(object);
						break;
					case "hackathon/id":
						res = hackathon_id(object);
						break;
					case "hackathons":
						res = hackathons(object);
						break;
				}

				if(res == null) {
					String e404 = "<html><head><title>404 Not Found!!!</title></head><body>404 not found</body></html>";
					t.sendResponseHeaders(404, e404.length());
					output.write(e404.getBytes("UTF-8"));
				} else {
					byte[] response = res.toString().getBytes("UTF-8");

					t.sendResponseHeaders(200, response.length);
					output.write(response);
				}
			} catch(Exception e) {
				String e400 = "<html><head><title>400 Invalid Request!!!</title></head><body>400 invalid request</body></html>";
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
		User user = server.users.get(username);
		if(user == null) {
			return new JSONObject("{\"code\":1, \"message\":\"Username/Password Incorrect\"}");
		}
		if(!user.verifyPassword(password.getBytes(Charset.forName("UTF-8")))) {
			return new JSONObject("{\"code\":1, \"message\":\"Username/Password Incorrect\"}");
		}

		int id;
		do {
			id = random.nextInt() & 0x7FFFFFFF;
		} while(server.session.containsKey(id));

		server.session.put(id, new Session(id, user));

		return new JSONObject("{\"code\":0, \"id\":" + id + "}");
	}

	public JSONObject register(JSONObject data) {
		JSONObject response = new JSONObject();
		String username = data.getString("username");
		String name = data.getString("name");
		String password = data.getString("password");
		String email = data.getString("email");

		if (server.users.get(username) == null){
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

	public JSONObject hackathon_join(JSONObject data){
        String username = data.getString("username");
        int hackID = data.getInt("hackathon_id");

        JSONObject response = new JSONObject();

        if (server.users.containsKey(username)){
            if (server.hackathons.containsKey(hackID)){
                server.users.get(username).joinedHackathons.put(hackID, server.hackathons.get(hackID));
                server.hackathons.get(hackID).users.put(server.users.get(username).id, server.users.get(username));

                response.put("code", 0);
                response.put("message", "User successfully joined hackathon");
            } else {
                response.put("code", 1);
                response.put("message", "Hackathon does not exist");
            }
        } else {
            response.put("code", 1);
            response.put("message", "User does not exist");
        }

		return response;
	}

	public JSONObject hackathon_id(JSONObject data) {
		int id = data.getInt("id");
		if(server.hackathons.containsKey(id)) {
			Hackathon h = server.hackathons.get(id);
			JSONObject array = new JSONObject();
			array.put("name", h.name);
			array.put("description", h.description);
			array.put("id", h.id);
			array.put("start", h.startTime);
			array.put("end", h.endTime);
			JSONArray events = new JSONArray();
			for(Event e : h.events.values()) {
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
		if(data.has("filter"))
			filter = data.getString("filter");

		JSONArray array = new JSONArray();

        for (Hackathon hack : server.hackathons.values()){
            if(hack.name.toLowerCase().contains(filter)) {
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

	public JSONObject create_hackathon(JSONObject data){
        String name = data.getString("hackathon_name");
        String description = data.getString("hackathon_description");
        long startTime = data.getLong("start_time");
        long endTime = data.getLong("end_time");

        JSONObject response = new JSONObject();

        if (name != null && description != null && startTime != 0 && endTime != 0){
            response.put("code", 0);
            response.put("message", "Hackathon successfully created.");
        } else {
            response.put("code", 1);
            response.put("message", "Insufficient/Invalid data provided.");
        }

        return response;
    }

    public JSONObject create_event(JSONObject data){
        int hackathon_id = data.getInt("hackathon_id");
        String name = data.getString("event_name");
        String description = data.getString("event_description");
        long startTime = data.getLong("start_time");
        long endTime = data.getLong("end_time");

        JSONObject response = new JSONObject();

        if (server.hackathons.containsKey(hackathon_id)){
            int eventID = server.hackathons.get(hackathon_id).events.size();
            server.hackathons.get(hackathon_id).events.put(
                    eventID,
                    new Event(eventID, startTime, endTime, name, description)
            );

            response.put("code", 0);
            response.put("message", "Event added successfully.");
        } else {
            response.put("code", 1);
            response.put("message", "Hackathon not found.");
        }

        return response;
    }

}
