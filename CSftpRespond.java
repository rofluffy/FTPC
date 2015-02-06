
public class CSftpRespond {
	
	private String respCode;
	private String respText;
	
	public CSftpRespond(String respCode, String respText){
		this.respCode = respCode;
		this.respText = respText;
	}
	
	public String getRespCode(){
		return this.respCode;
	}
	
	public String getRespText(){
		return this.respText;
	}

}
