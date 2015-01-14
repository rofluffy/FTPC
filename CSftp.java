
import java.lang.System;
import java.io.IOException;

//
// This is an implementation of a simplified version of a command 
// line ftp client. The program takes no arguments.
//


public class CSftp
{
    static final int MAX_LEN = 255;

    public static void main(String [] args)
    {
	byte cmdString[] = new byte[MAX_LEN];
	try {
	    for (int len = 1; len > 0;) {
		System.out.print("csftp> ");
		len = System.in.read(cmdString);
		if (len <= 0) 
		    break;
		// Start processing the command here.
		System.out.println("900 Invalid command.");
	    }
	} catch (IOException exception) {
	    System.err.println("998 Input error while reading commands, terminating.");
	}
    }
}
