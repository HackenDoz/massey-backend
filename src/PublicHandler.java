import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class PublicHandler implements HttpHandler {

	public transient final Server server;

	public PublicHandler(Server server) {
		this.server = server;
	}

	public void handle(HttpExchange httpExchange) throws IOException {
		String uri = httpExchange.getRequestURI().toString();
		if(uri.endsWith("/")) {
			uri += "index.html";
		}
		File f = new File("public", uri);
		OutputStream output = httpExchange.getResponseBody();
		if(!f.exists()) {
			String e404 = "<html><head><title>404 Not Found!!!</title></head><body>404 not found</body></html>";
			httpExchange.sendResponseHeaders(404, e404.length());
			output.write(e404.getBytes());
		} else {
			httpExchange.sendResponseHeaders(200, f.length());
			FileInputStream input = new FileInputStream(f);
			byte[] buffer = new byte[4096];
			int read;
			while((read = input.read(buffer)) != -1) {
				output.write(buffer, 0, read);
			}
			input.close();
		}
		output.close();
	}
}
