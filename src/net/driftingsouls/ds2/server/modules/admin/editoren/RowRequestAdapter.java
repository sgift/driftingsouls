package net.driftingsouls.ds2.server.modules.admin.editoren;

import net.driftingsouls.ds2.server.framework.pipeline.Request;
import org.apache.commons.fileupload.FileItem;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

class RowRequestAdapter implements Request
{
	private final Request inner;
	private final Map<String,String> parameters;

	RowRequestAdapter(Request inner, Map<String, String> parameters)
	{
		this.inner = inner;
		this.parameters = parameters;
	}

	@Nonnull
	@Override
	public String[] getParameterValues(@Nonnull String parameter)
	{
		return parameters.containsKey(parameter) ?
				new String[] {parameters.get(parameter)} :
				new String[0];
	}

	@Override
	public String getParameter(String parameter)
	{
		return parameters.get(parameter);
	}

	@Override
	public String getParameterString(String parameter)
	{
		return parameters.getOrDefault(parameter, "");
	}

	@Override
	public int getParameterInt(String parameter)
	{
		try {
			return Integer.parseInt(parameters.get(parameter));
		}
		catch( NumberFormatException e ) {
			return 0;
		}
	}

	@Override
	public void setParameter(String parameter, String value)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getContentType()
	{
		return inner.getContentType();
	}

	@Override
	public InputStream getInputStream() throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getQueryString()
	{
		return inner.getQueryString();
	}

	@Override
	public String getPath()
	{
		return inner.getPath();
	}

	@Override
	public String getCharacterEncoding()
	{
		return inner.getCharacterEncoding();
	}

	@Override
	public int getContentLength()
	{
		return inner.getContentLength();
	}

	@Override
	public String getHeader(String header)
	{
		return inner.getHeader(header);
	}

	@Override
	public String getRemoteAddress()
	{
		return inner.getRemoteAddress();
	}

	@Override
	public String getRequestURL()
	{
		return inner.getRequestURL();
	}

	@Override
	public String getUserAgent()
	{
		return inner.getUserAgent();
	}

	@Override
	public List<FileItem> getUploadedFiles()
	{
		return inner.getUploadedFiles();
	}

	@Override
	public <T> T getFromSession(Class<T> cls)
	{
		return inner.getFromSession(cls);
	}

	@Override
	public void removeFromSession(Class<?> cls)
	{
		inner.removeFromSession(cls);
	}

	@Override
	public String getCookie(String name)
	{
		return inner.getCookie(name);
	}

	@Override
	public Map<String, String> getParameterMap()
	{
		return parameters;
	}
}
