import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class Main {

	public static void main(String[] args) throws Throwable{

		File f = new File("server.dat");

		Server server = null;

		try {
			if (f.exists()) {
				ObjectInputStream input = new ObjectInputStream(new FileInputStream(f));
				server = (Server) input.readObject();
				input.close();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}

		if(server == null) {
			server = new Server();
		}

		server.connect(new InetSocketAddress(8080));

		final Save saveThread = new Save(server, f);

		new Thread() {
			public void run() {
				while(true) {
					saveThread.run();
					try {
						Thread.sleep(10000);
					}
					catch(InterruptedException e) {}
				}
			}
		}.start();

		Runtime.getRuntime().addShutdownHook(saveThread);


	}

	public static class Save extends Thread {
		public Server server;
		public File file;
		public Save(Server server, File f) {
			this.server = server;
			this.file = f;
		}
		public void run() {
			System.out.println("Saving");
			try {
				ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(file));
				output.writeObject(server);
				output.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
}
