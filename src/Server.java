import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;

public class Server {

	public HashMap<Integer, Hackathon> hackathons = new HashMap<>();
	public HashMap<String, User> users = new HashMap<>();
	public HashMap<Integer, Session> session = new HashMap<>();

	public Server(InetSocketAddress address) throws IOException {
		HttpServer server = HttpServer.create(address, 0);
		server.createContext("/api/", new APIHandler(this));
		server.createContext("/", new PublicHandler(this));
		server.setExecutor(null);
		server.start();
	}


}
