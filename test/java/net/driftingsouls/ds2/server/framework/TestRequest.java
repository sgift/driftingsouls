package net.driftingsouls.ds2.server.framework;

import net.driftingsouls.ds2.server.framework.pipeline.Request;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Testrequest zur Verwendung in Testfaellen.
 */
public class TestRequest implements Request
{
	private Log log = LogFactory.getLog(TestRequest.class);

	private Map<String,String> params = new HashMap<>();

	/**
	 * Erstellt ein neues Request-Objekt.
	 * @param params Die Parameter
	 */
	public TestRequest(Map<String,String> params)
	{
		params.putAll(params);
	}

	/**
	 * Erstellt ein neues Request-Objekt mit genau einem Parameter.
	 * @param paramKey Der Name des Parameters
	 * @param paramValue Der Wert des Parameters
	 */
	public TestRequest(String paramKey, String paramValue) {
		params.put(paramKey, paramValue);
	}

	@Override
	public String getCharacterEncoding() {
		return "UTF-8";
	}

	@Override
	public int getContentLength() {
		return 0;
	}

	@Override
	public String getContentType() {
		return "text";
	}

	@Override
	public String getHeader(String header) {
		return null;
	}

	@Override
	public InputStream getInputStream() throws IOException
	{
		return null;
	}

	@Nonnull
	@Override
	public String[] getParameterValues(@Nonnull String parameter)
	{
		String val = params.get(parameter);
		if( val == null )
		{
			return new String[0];
		}
		return new String[] {val};
	}

	@Override
	public String getParameter(String parameter) {
		return params.get(parameter);
	}

	@Override
	public int getParameterInt(String parameter) {
		String param = params.get(parameter);
		if( param == null ) {
			return 0;
		}
		try {
			return Integer.parseInt(param);
		}
		catch( NumberFormatException e ) {
			// EMPTY
		}
		return 0;
	}

	@Override
	public String getParameterString(String parameter) {
		String param = params.get(parameter);
		if( param == null ) {
			return "";
		}
		return param;
	}

	@Override
	public String getPath() {
		return System.getProperty("user.dir");
	}

	@Override
	public String getQueryString() {
		return "";
	}

	@Override
	public String getRemoteAddress() {
		return "localhost";
	}

	@Override
	public String getRequestURL() {
		return "./java";
	}

	@Override
	public String getUserAgent() {
		return "TestCase";
	}

	@Override
	public void setParameter(String parameter, String value) {
		params.put(parameter, value);
	}

	@Override
	public List<FileItem> getUploadedFiles() {
		return new ArrayList<>();
	}

	@Override
	public <T> T getFromSession(Class<T> cls) {
		log.error("getFromSession not supported");

		return null;
	}

	@Override
	public void removeFromSession(Class<?> cls) {
		log.error("removeFromSession not supported");
	}

	@Override
	public String getCookie(String name) {
		return null;
	}

	@Override
	public Map<String, String> getParameterMap()
	{
		return Collections.unmodifiableMap(this.params);
	}
}
