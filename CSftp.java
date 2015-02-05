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
	private static String host = null;
	private static int port = 21;
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
				//System.out.println("cmdString: " + cmd);
				control(cmd);	            
				//System.out.println("800 Invalid command."); // this is the default
			}
			
		} catch (UnknownHostException exception) {
			System.err.println("Don't know about host"); //TODO
		} catch (IOException exception) {
			System.err
					.println("898 Input error while reading commands, terminating.");
			System.exit(1);
		} catch (NullPointerException e){
			e.printStackTrace();
			System.err
			.println("898 Input error while reading commands, terminating.");
			System.exit(1);
		}
	}

	// function control the cmd
	public static void control(String cmd) {
		String[] input = cmd.split("\\s+");
		control = input[0];
		//System.out.println("Check input: length" + input.length + " Control: " + control);
		try {
			switch (control) {
			case "open":
				if (input.length > 3 || input.length == 1){
					throw new IOException("801 Incorrect number of arguments.");
				} else if (input.length == 3){
					host = input[1];
					port = Integer.parseInt(input[2]);
					connect(host, port);
				} else {
					host = input[1];
					connect(host);
				}
				break;
				
			case "close":
				if (input.length != 1){
					throw new IOException("801 Incorrect number of arguments.");
				}
				logout();
				break;

			case "quit":
				if (input.length != 1){
					throw new IOException("801 Incorrect number of arguments.");
				}
				disconnect();
				break;
				
			case "get":
				if(input.length != 2){
					throw new IOException("801 Incorrect number of arguments.");
				}else{
					getFile(input[1]);
				}
				break;
			
			case "put":
				if(input.length != 2){
					throw new IOException("801 Incorrect number of arguments.");
				}else{
					putFile(input[1]);
				}
				break;
			
			case "cd":
				if(input.length != 2){
					throw new IOException("801 Incorrect number of arguments.");
				}else {
					changedir(input[1]);
				}
				break;
			
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
	
	
	// open SERVER PORT (port is optional, default is 21, return 803 if already open)
	public static void connect(String host) throws IOException{
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
			
			//System.out.println("Successfully connected to " + socket);
//			String response = getLine();
//			if (!response.startsWith("220")) {
//				socket.close();
//				socket = null;
//				throw new IOException("820 Control connection to " + host
//						+ " on port " + port + " failed to open.");
//			}
//			while (response != null){
//				System.out.print("<--" + response);
//				//break;
//			}
			
			printLine();
			//System.out.println("response was empty continue");
			
			login();

		}catch (SocketException e){
			System.err.println("820 Control connection to " + host + " on port " +  port + " failed to open.");
			socket = null;
		}catch (UnknownHostException e) {
			System.err.println("820 Control connection to " + host + " on port " + port + " failed to open.");
			socket = null;
		}

	}

	// user USERNAME
	public static void login() throws IOException {
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		System.out.println("Name: ");
		BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
		String user = userInput.readLine();
		
		toServer("USER " + user);
		String response = getLine();
		if (response.startsWith("331")){
			// password is needed
			System.out.println("<-- " + response);
			pwInput();
		} else if (response.startsWith("230")){
			// password is not needed
			System.out.println("<-- " + response);
		} else {
			// not success case
			socket.close();
			socket = null;
			throw new IOException("<-- " + response);
		}

	}
	
	public static void pwInput() throws IOException{
		
		System.out.println("password: ");
		BufferedReader userInput = new BufferedReader(new InputStreamReader(
				System.in));
		String pass = userInput.readLine();
		toServer("PASS " + pass);

		// response = reader.readLine();
		String response = getLine();
		if (response.startsWith("230")) {
			System.out.println("<-- " + response);
		} else {
			socket.close();
			socket = null;
			throw new IOException("<-- " + response);
		}
	}

	// TODO: close (close current connected server)
	public static void logout() throws IOException, NullPointerException {
		if (socket == null || socket.isClosed()){
			throw new IOException("803 Supplied command not expected at this time.");
		}
		try {
			toServer("QUIT");
			String response = getLine();
			socket.close();
			socket = null;
			if (!response.startsWith("221")){
				System.out.println("<-- " + response);
				throw new IOException("803 Supplied command not expected at this time.");
			} else {
				System.out.println("<-- " + response);
			}
		} catch (SocketException e){
			System.err.println("803 Supplied command not expected at this time.");
		}
	}

	// TODO: quit (quit everything no matter what)
	public static void disconnect() throws IOException{
		if (socket == null || socket.isClosed()){
			reader.close();
			writer.close();
			System.exit(0);
		}
		try {
			toServer("QUIT");
			String response = getLine();
			socket.close();
			socket = null;
			System.out.println("<-- " + response);
			reader.close();
			writer.close();
			System.exit(0);
		} catch (IOException e){
			System.err.println("898 Input error while reading commands, terminating.");
		} finally {
			reader.close();
			writer.close();
			System.exit(0);
		}
	}

	// TODO: get REMOTE (get file from server)
	@SuppressWarnings("resource")
	public static void getFile(String fileName) throws IOException{
		// turn binary flag on
		toServer("TYPE I");
		String response = getLine();
		if (!response.startsWith("200")){
			throw new IOException("<-- " + response);
		}
		System.out.print("<-- " + response);
		// open passive mode, get the server address back
		toServer("PASV");
		response = reader.readLine();
		if (!response.startsWith("227")){
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
		
		Socket transferSocket = null;
		try {
			transferSocket = new Socket(host, port);
		}catch (SocketTimeoutException e){
			System.err.println("830 Data transfer connection to " + host + " on port " + port + " failed to open.");
		}
		
		// send RETR with file name
		toServer("RETR " + fileName);
		response = getLine();
		if(!(response.startsWith("125") || response.startsWith("150"))){
			throw new IOException("810 Access to local file " + fileName + " denied. ");
		} else {
			System.out.println("<-- " + response);
		}
		
		try {
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
		
		response = getLine();
		System.out.println("<-- " + response);
		
		} catch (SocketException e){
			System.err.println("825 Control connection I/O error, closing control connection.");
			socket.close();
			socket = null;
		} catch (FileNotFoundException e){
			System.err.println("810 Access to local file " + fileName + " denied.");
		} catch (IOException e){
			System.err.println("835 Data transfer connection I/O error, closing data connection. ");
			socket.close();
			socket = null;
		}
		
	}

	// TODO: put LOCAL (put local file to server)
	@SuppressWarnings("resource")
	public static void putFile(String fileName) throws IOException {
		try {
		// turn binary flag on
		toServer("TYPE I");
		String response = getLine();
		if (!response.startsWith("200")){
			throw new IOException("<-- " + response);
		}
		System.out.print("<-- " + response);
		// open passive mode, get the server address back
		toServer("PASV");
		response = reader.readLine();
		if (!response.startsWith("227")){
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

		Socket transferSocket = null;
		try {
			transferSocket = new Socket(host, port);
		}catch (SocketTimeoutException e){
			System.err.println("830 Data transfer connection to " + host + " on port " + port + " failed to open.");
		}

		// send STOR with file name
		toServer("STOR " + fileName);
		response = getLine();
		if(!(response.startsWith("125") || response.startsWith("150"))){
			throw new IOException("810 Access to local file " + fileName + " denied. ");
		} else {
			System.out.println("<-- " + response);
		}
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
		
		//printLine();
		response = getLine();
		System.out.println("<-- " + response);
		
		} catch (SocketException e){
			System.err.println("825 Control connection I/O error, closing control connection.");
			socket.close();
			socket = null;
		} catch (FileNotFoundException e){
			System.err.println("810 Access to local file " + fileName + " denied.");
		}
	}

	// TODO: cd DIRECTORY (change curr working dir) CHANGE the error printing
	public static void changedir(String path) throws IOException {
		toServer("CWD " + path);
		String response = getLine();
		if (!response.startsWith("250")) {
			throw new IOException("<-- " + response);
		} else {
			System.out.println("<-- " + response);
		}

		// print the current directory path
		toServer("PWD");
		response = getLine();
		if (!response.startsWith("257")) {
			System.out.println("<-- " + response);
			throw new IOException("not right lol.");
		} else {
			System.out.println("<-- " + response);
		}
	}

	// TODO: dir (list of file in working dir on server)
	@SuppressWarnings("resource")
	public static void dirlist() throws IOException {
		// enter the passive mode and get the port address
		toServer("PASV");
		String response = reader.readLine();
		if (!response.startsWith("227")){
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
		
		Socket transferSocket = null;
		try {
			transferSocket = new Socket(host, port);
		}catch (SocketTimeoutException e){
			System.err.println("830 Data transfer connection to " + host + " on port " + port + " failed to open.");
		}
		
		// send LIST command
		toServer("LIST");
		BufferedReader listReader = new BufferedReader(new InputStreamReader(transferSocket.getInputStream()));
		response = getLine();
		if (!(response.startsWith("125") || response.startsWith("150"))){
			throw new IOException("<-- " + response);
		}
		System.out.println("<-- " + response);
		String list = null;
		while ((list = listReader.readLine()) != null){
			System.out.println(list);
		}
		
		listReader.close();
		transferSocket.close();

		//printLine();
		response = getLine();
		System.out.println("<-- " + response);

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
	
//	private static void printLine() throws IOException{
//		String response = null;
//		do {
//			response = reader.readLine();
//			System.out.println("<-- " + response);
//		}while(reader.ready());
//		
//	}
	
	private static void printLine() throws IOException{
		String response = null;
		do {
			response = reader.readLine();
			if (!response.startsWith("220")){
				socket.close();
				socket = null;
				throw new IOException("820 Control connection to " + host
						+ " on port " + port + " failed to open.");
			}
			System.out.println("<-- " + response);
		}while(reader.ready());
		
	}
	
	private static String getLine() throws IOException{
		//reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		String line = null;
		StringBuffer sb = new StringBuffer();
		do {
			line = reader.readLine();
			//System.out.println("reading line");
			sb.append(line).append("\r\n");
			if (line == null){
				socket.close();
				socket = null;
				throw new IOException("899 Processing error. The server is not responding.");
			}
		}while(reader.ready());
		//reader.close();
		String ret = sb.toString();
		return ret;	
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
		//System.out.println("the data address is: "+ aString);
		return address;
	}

}
