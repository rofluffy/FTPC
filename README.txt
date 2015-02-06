
- when using ftp.cisco.com, the welcome message is too long and bufferedReader cannot readLine in time, thus when user input [USER] username, the respond is still reading the previous 220 responds and the program results in detecting the wrong respond.

- fixed above issue

- "PWD" is also included after "CWD" sent for checking (testing) purpose