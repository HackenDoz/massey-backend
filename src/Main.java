import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class Main {

	public static void main(String[] args) throws Throwable{
		new Server(new InetSocketAddress(8080));

	}
}
