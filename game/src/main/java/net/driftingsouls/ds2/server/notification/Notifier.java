package net.driftingsouls.ds2.server.notification;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStreamWriter;
import java.net.URL;

public class Notifier {

	private final String apiKey;

	public Notifier(final String apiKey) {
		this.apiKey = apiKey;
	}

	public int sendMessage(final String title, final String text){
		try {
			final URL url =    new URL("https://www.notifymydevice.com/push");
            final HttpsURLConnection conn= (HttpsURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
						conn.setDoOutput(true);

					String inhalt = parseToValidText(text);
					String titel = parseToValidText(title);

	        final OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
	        out.write("ApiKey=");
	        out.write(apiKey);
	        out.write("&PushTitle=");
	        out.write(titel);
	        out.write("&PushText=");
	        out.write(inhalt);
	        out.flush();
	        out.close();


	        return conn.getResponseCode();
	    }
		catch (final Exception anException){
			anException.printStackTrace();
		}
		return -1;
	}

	private String parseToValidText(String input){
		String output = input;

		//html-Tags entfernen
		output = output.replaceAll("\\<. *?>","");
		//BB Code entfernen
		output = output.replaceAll("\\[\\/?(?i)(b|i|u|color|url|img|email|size|list|font|youtube|ship|map|base|resource|userprofile|align|mark|\\*){1}=?[^\\]]*\\]","");
		//&-Symbol entfernen, das ist in URLs leider nicht erlaubt
		output = output.replace("&"," und ");

		return output;
	}


}
