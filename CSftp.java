import java.lang.System;
import java.io.*;
import java.net.*;

//
// This is an implementation of a simplified version of a command 
// line ftp client. The program takes no arguments.
//

public class CSftp {
	static final int MAX_LEN = 255;
	private static Socket socket = null;
	private static BufferedReader reader = null;
	private static PrintWriter writer = null;
	
	private BufferedReader  dataReader = null;
	private PrintWriter dataWriter = null;
	
	static String control = null;

	public static void main(String[] args) {
		byte cmdString[] = new byte[MAX_LEN];
		try {
			for (int len = 1; len > 0;) {
				System.out.print("csftp> ");
				len = System.in.read(cmdString);
				String cmd = new String(cmdString, 0, len-1);
				if (len <= 0)
					break;				
				
				// Start processing the command here.
				System.out.println("cmdString: " + cmd);
				control(cmd);	            
				//System.out.println("800 Invalid command."); // this is the default
			}
			
		} catch (UnknownHostException exception) {
			System.err.println("Don't know about host "); //TODO
		} catch (IOException exception) {
			System.err
					.println("898 Input error while reading commands, terminating.");
			//System.exit(1);
		} /*catch (NullPointerException exception){
			System.err.println("899 Processing error. I don't know what's going on either :(. ");
		}*/
	}

	// TODO: function control the cmd
	public static void control(String cmd) {
		String[] input = cmd.split("\\s+");
		control = input[0];
		System.out.println("Check input: length" + input.length + " Control: " + control);
		try {
			switch (control) {
			case "open":
				if (input.length > 3 || input.length == 1){
					throw new IOException("801 Incorrect number of arguments.");
				} else if (input.length == 3){
					connect(input[1], Integer.parseInt(input[2]));
				} else {
					connect(input[1]);
				}
				break;
				
			case "close":
				logout();
				break;

			case "quit":
				disconnect();
				break;
				
			// TODO: get
			// TODO: put
			// TODO: cd
			// TODO: dir

			default:
				System.out.println("800 Invalid command.");
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NumberFormatException e){
			System.err.println("802 Invalid argument.");
		}

	}
	// TODO: open SERVER PORT (port is optional, default is 21, return 803 if already open)
	public static void connect(String host) throws IOException{
		// TODO: if open, close, quit, get blablabla...
		if (socket != null){
			throw new IOException("803 Supplied command not expected at this time.");
		}
		connect(host, 21);
	}
	
	public static void connect(String host, int port) throws IOException{
		if (socket != null){
			throw new IOException("803 Supplied command not expected at this time.");
		}
		try{
			socket = new Socket(host, port);
			writer = new PrintWriter(socket.getOutputStream(), true);
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			System.out.println("Successfully connected to " + socket);			
			String response = reader.readLine();
			if (!response.startsWith("220 ")){
				throw new IOException("820 Control connection to " + host + " on port " +  port + " failed to open.");
			} else {
			System.out.println(response);
			}
			System.out.print("Name: ");
			BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            login(userInput.readLine());

		}catch (IOException e){
			System.err.println("820 Control connection to " + host + " on port " +  port + " failed to open.");
		}
	}

	// TODO: user USERNAME
	public static void login(String user){
		try {
			writer = new PrintWriter(socket.getOutputStream(), true);
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			toServer("USER " + user);
			String response = reader.readLine();
			if (!response.startsWith("331 ")) {
				throw new IOException("login fail" + response);
            }
			System.out.println("Server: " + response);
			System.out.println("password: ");
			
			BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
			String pass = userInput.readLine();
			toServer("PASS " + pass);
			
			System.out.println("before reader update: " + response);
			response = reader.readLine();
			System.out.println("before if check: " + response);
			
			if (!response.startsWith("230 ")){
				System.out.println("after if check: " + response);
				socket = null;
				throw new IOException("login fail. " + response);
				// TODO we have a nullpointer here if user name does not match with password
			} else {
				System.out.println("Server: " + response);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// TODO: close (close current connected server)
	public static void logout() throws IOException {
		if (socket == null){
			throw new IOException("803 Supplied command not expected at this time.");
		}
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		toServer("QUIT");
		// TODO socket.close(); what does this do?
		socket = null;
		String response = reader.readLine();
		if (!response.startsWith("221 ")){
			System.out.println("Server: " + response);
			throw new IOException("803 Supplied command not expected at this time.");
		} else {
			System.out.println("Server: " + response);
		}
	}

	// TODO: quit (quit everything no matter what)
	public static void disconnect(){
		try {
			if (socket == null){
				System.exit(1);
			};
			toServer("QUIT");
			String response = reader.readLine();
			System.out.println(response);
			System.exit(1);
		} catch (IOException e){
			System.err.println("899 Processing error.");
		}
	}

	// TODO: get REMOTE (get file from server)

	// TODO: put LOCAL (put local file to server)

	// TODO: cd DIRECTORY (change curr working dir)

	// TODO: dir (list of file in working dir on server)
	
	// helper to server
	private static void toServer(String input) throws IOException {
		if (socket == null){
			throw new IOException("803 Supplied command not expected at this time. Please connect to a server");
		}
		writer.write(input + "\r\n");
		writer.flush();
		if (writer != null){
			System.out.println("--> "+ input);
		}
	}

}
