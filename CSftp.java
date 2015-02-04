import java.lang.System;
import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

//
// This is an implementation of a simplified version of a command 
// line ftp client. The program takes no arguments.
//

public class CSftp {
	static final int MAX_LEN = 255;
	private static Socket socket = null;
	private static BufferedReader reader = null;
	private static PrintWriter writer = null;
	
	private BufferedReader dataReader = null;
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
			System.err.println("Don't know about host"); //TODO
		} catch (IOException exception) {
			System.err
					.println("898 Input error while reading commands, terminating.");
		} /*catch (NullPointerException exception){
			System.err.println("803 Supplied command not expected at this time.");
		}*/
	}

	// TODO multi line response fix!
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
			case "get":
				if(input.length != 2){
					throw new IOException("801 Incorrect number of arguments.");
				}else{
					getFile(input[1]);
				}
				break;
			// TODO: put
			case "put":
				if(input.length != 2){
					throw new IOException("801 Incorrect number of arguments.");
				}else{
					putFile(input[1]);
				}
				break;
			// TODO: cd
			case "cd":
				if(input.length != 2){
					throw new IOException("801 Incorrect number of arguments.");
				}else {
					changedir(input[1]);
				}
				break;
			// TODO: dir
			case "dir":
				if(input.length>1){
					throw new IOException("801 Incorrect number of arguments.");
				}else {
					dirlist();
				}
				break;
				
			case "":
				break;

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

	// user USERNAME
	public static void login(String user){
		try {
			toServer("USER " + user);
			String response = reader.readLine();
			if (!response.startsWith("331 ")) {
				socket = null;
				socket.close();
				throw new IOException("login fail" + response);
            }
			System.out.println("Server: " + response);
			System.out.println("password: ");
			
			BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
			String pass = userInput.readLine();
			toServer("PASS " + pass);

			response = reader.readLine();
			if (response.startsWith("230 ")){
				System.out.println("Server: " + response);
			} else if (response.startsWith("230-")){
				System.out.println("Server: " + response);
			} else {
				System.out.println("after if check: " + response);
				socket = null;
				socket.close();
				throw new IOException("login fail. " + response);
				// TODO we have a nullpointer here if user name does not match with password
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException e){
			e.printStackTrace();
		}
	}

	// TODO: close (close current connected server)
	public static void logout() throws IOException, NullPointerException {
		try {
			toServer("QUIT");
			String response = reader.readLine();
			socket.close();
			socket = null;
			if (!response.startsWith("221 ")){
				System.out.println("Server: " + response);
				throw new IOException("803 Supplied command not expected at this time.");
			} else {
				System.out.println("Server: " + response);
			}
		} catch (SocketException e){
			System.err.println("803 Supplied command not expected at this time.");
		}
	}

	// TODO: quit (quit everything no matter what)
	public static void disconnect(){
		try {
			if (socket == null || socket.isClosed()){
				System.exit(1);
			};
			toServer("QUIT");
			String response = reader.readLine();
			socket.close();
			System.out.println(response);
			System.exit(1);
		} catch (IOException e){
			System.err.println("899 Processing error.");
		}
	}

	// TODO: get REMOTE (get file from server)
	public static void getFile(String fileName) throws IOException{
		// turn binary flag on
		toServer("TYPE I");
		String response = reader.readLine();
		System.out.print("<-- " + response);
		// open passive mode, get the server address back
		toServer("PASV");
		response = reader.readLine();
		if (!response.startsWith("227 ")){
			throw new IOException("<-- " + response);
		} else {
			System.out.println("<-- " + response);
		}
		String[] address = getDataAddress(response);
		
		String host = address[0];
		for (int i=1;i<4;i++){
			host += "." + address[i];
		}
		
		int port = Integer.parseInt(address[4])* 256 + Integer.parseInt(address[5]);
		
		Socket transferSocket = new Socket(host, port);
		
		// send RETR with file name
		toServer("RETR " + fileName);
		FileOutputStream fileOutput = new FileOutputStream(fileName);
		InputStream fileInput = transferSocket.getInputStream();
		byte[] dataBuffer = new byte[4096];
		int len = -1;
		while ((len = fileInput.read(dataBuffer)) > -1){
			fileOutput.write(dataBuffer, 0, len);
		}
		fileOutput.flush();
		fileOutput.close();
		fileInput.close();
		transferSocket.close();
		
		printLine();
		
	}

	// TODO: put LOCAL (put local file to server)
	public static void putFile(String fileName) throws IOException {
		// turn binary flag on
		toServer("TYPE I");
		String response = reader.readLine();
		System.out.print("<-- " + response);
		// open passive mode, get the server address back
		toServer("PASV");
		response = reader.readLine();
		if (!response.startsWith("227 ")) {
			throw new IOException("<-- " + response);
		} else {
			System.out.println("<-- " + response);
		}
		String[] address = getDataAddress(response);

		String host = address[0];
		for (int i = 1; i < 4; i++) {
			host += "." + address[i];
		}

		int port = Integer.parseInt(address[4]) * 256 + Integer.parseInt(address[5]);

		Socket transferSocket = new Socket(host, port);

		// send STOR with file name
		toServer("STOR " + fileName);
		FileInputStream fileInput = new FileInputStream(fileName);
		OutputStream fileOutput = transferSocket.getOutputStream();
		byte[] dataBuffer = new byte[4096];
		int len = -1;
		while((len = fileInput.read(dataBuffer)) > -1){
			fileOutput.write(dataBuffer, 0, len);
		}
		fileOutput.flush();
		fileOutput.close();
		fileInput.close();
		transferSocket.close();
		
		printLine();
	}

	// TODO: cd DIRECTORY (change curr working dir) CHANGE the error printing
	public static void changedir(String path) throws IOException {
		try {
			toServer("CWD " + path);
			String response = reader.readLine();
			if (response.startsWith("550 ")) {
				System.out.println("Server: " + response);
				throw new IOException(
						"803 Supplied command not expected at this time.");
			} else {
				System.out.println("Server: " + response);
			}
			
			// print the current directory path
			toServer("PWD");
			response = reader.readLine();
			if (!response.startsWith("257 ")) {
				System.out.println("<-- " + response);
				throw new IOException("not right lol.");
			} else {
				System.out.println("<-- " + response);
			}

		} catch (IOException e) {
			// e.printStackTrace();
			System.err.println("899 Processing error.");
		}

	}

	// TODO: dir (list of file in working dir on server)
	public static void dirlist() throws IOException {
		// enter the passive mode and get the port address
		toServer("PASV");
		String response = reader.readLine();
		if (!response.startsWith("227 ")){
			throw new IOException("<-- " + response);
		} else {
			System.out.println("<-- " + response);
		}
		String[] address = getDataAddress(response);
		
		String host = address[0];
		for (int i=1;i<4;i++){
			host += "." + address[i];
		}
		
		int port = Integer.parseInt(address[4])* 256 + Integer.parseInt(address[5]);
		
		Socket transferSocket = new Socket(host, port);
		// send LIST command
		toServer("LIST");
		BufferedReader listReader = new BufferedReader(new InputStreamReader(transferSocket.getInputStream()));
		response = reader.readLine();
		System.out.println("<-- " + response);
		String list = null;
		while ((list = listReader.readLine()) != null){
			System.out.println(list);
		}
		
		listReader.close();
		transferSocket.close();

		// get the current directory to check
		//toServer("PWD");
		printLine();

	}
	
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
	
	private static void printLine() throws IOException{
		String response = null;
		do {
			response = reader.readLine();
			System.out.println("<-- " + response);
		}while(reader.ready());
		
	}
	
	private String getLine(){
		String line = null;
		
		return line;	
	}
	
	private static String[] getDataAddress(String response) throws IOException{
		
		String[] address = new String[6];
		int startIndex = response.indexOf("(");
		int endIndex = response.indexOf(")");
		if (startIndex < 0 || endIndex <0){
			throw new IOException("835 Data transfer connection I/O error, closing data connection.");
		}
		
		String aString = response.substring(startIndex+1, endIndex);
		StringTokenizer st = new StringTokenizer(aString, ",");
		
		for (int i=0;i<6;i++){
			address[i] = st.nextToken();
		}
		System.out.println("the data address is: "+ aString);
		return address;
	}

}
