package net.driftingsouls.ds2.server.notification;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class Notifier {

	final String apiKey;  
	 
	public Notifier(final String apiKey_) {
		apiKey = apiKey_; 
	}  
	 
	public int sendMessage(final String title, final String text){  
		try {     
			final URL url =    new URL("https://www.notifymydevice.com/push");  
            final HttpsURLConnection conn= (HttpsURLConnection)url.openConnection(); 
            conn.setRequestMethod("POST");             
            conn.setDoOutput(true);  
	 
	        final OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());   
	        out.write("ApiKey=");        
	        out.write(apiKey);          
	        out.write("&PushTitle=");      
	        out.write(title);         
	        out.write("&PushText=");        
	        out.write(text);         
	        out.flush();        
	        out.close();  
	 
	        
	        return conn.getResponseCode();    
	    } 
		catch (final IOException anException){   
			anException.printStackTrace();        
		}      
		return -1;     
	}
	
	
}
