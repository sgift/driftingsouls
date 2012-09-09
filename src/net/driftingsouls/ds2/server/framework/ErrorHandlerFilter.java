package net.driftingsouls.ds2.server.framework;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import net.driftingsouls.ds2.server.framework.authentication.TickInProgressException;
import net.driftingsouls.ds2.server.user.authentication.AccountInVacationModeException;
import net.sf.json.JSONObject;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.StaleStateException;
import org.hibernate.exception.GenericJDBCException;

/**
 * Filter, um zentral alle Fehler abzufangen.
 * Spezielle, bekannte Exceptions werden hier extra behandelt.
 * Allgemeine Exceptions werden per Mail an die Entwickler versendet.
 * Die Spieler erhalten eine Fehlerseite.
 *
 * @author Drifting-Souls Team
 */
public class ErrorHandlerFilter implements Filter
{
	private static Log log = LogFactory.getLog(ErrorHandlerFilter.class);

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		try
		{
			chain.doFilter(request, response);
		}
		catch(Exception e)
		{
			boolean json = "JSON".equals(request.getParameter("FORMAT"));
			ErrorReporter reporter;
			if( json )
			{
				reporter = new JsonErrorReporter(request, response);
			}
			else
			{
				reporter = new HtmlErrorReporter(request, response);
			}
			Throwable ex = e;
			do
			{
				if(ex instanceof TickInProgressException)
				{
					reporter.reportTickInProgress((TickInProgressException)ex);
					return;
				}
				else if(ex instanceof StaleStateException )
				{
					reporter.reportStaleState((StaleStateException)ex);
					return;
				}
				else if((ex instanceof GenericJDBCException) && (((GenericJDBCException)ex).getSQLException().getMessage() != null) && ((GenericJDBCException)ex).getSQLException().getMessage().startsWith("Beim Warten auf eine Sperre wurde die") )
				{
					reporter.reportSqlLock((GenericJDBCException)ex);
					return;
				}
				else if(ex instanceof NotLoggedInException)
				{
					reporter.reportNotLoggedIn((NotLoggedInException)ex);
					return;
				}
				else if(ex instanceof AccountInVacationModeException)
				{
					reporter.reportInVacation((AccountInVacationModeException)ex);
					return;
				}
				ex = ExceptionUtils.getCause(ex);
			}
			while(ex != null);

			StringBuilder infos = new StringBuilder(100);
			HttpServletRequest req = (HttpServletRequest)request;
			Map<String, String[]> params = req.getParameterMap();
			for(Map.Entry<String, String[]> param: params.entrySet())
			{
				infos.append(param.getKey());
				infos.append(" => ");
				String[] values = param.getValue();
				for(int i = 0; i < values.length; i++)
				{
					infos.append(values[i]);
					if(i != values.length - 1)
					{
						infos.append(" || ");
					}
				}
				infos.append("\n");
			}

			Throwable mailThrowable = e;
			while( ExceptionUtils.getCause(mailThrowable) != null ) {
				mailThrowable = ExceptionUtils.getCause(mailThrowable);
			}

			Common.mailThrowable(mailThrowable, "Unexpected exception", infos.toString());
			log.info("", e);

			reporter.reportUnexpected(e);
		}
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException
	{}

	@Override
	public void destroy()
	{}
	
	private interface ErrorReporter
	{
		public void reportTickInProgress(TickInProgressException e) throws IOException;
		public void reportStaleState(StaleStateException e) throws IOException;
		public void reportSqlLock(GenericJDBCException e) throws IOException;
		public void reportNotLoggedIn(NotLoggedInException e) throws IOException;
		public void reportInVacation(AccountInVacationModeException e) throws IOException;
		public void reportUnexpected(Throwable t) throws IOException;
	}
	
	private class HtmlErrorReporter implements ErrorReporter
	{
		private ServletRequest request;
		private ServletResponse response;
		
		HtmlErrorReporter(ServletRequest request, ServletResponse response)
		{
			this.request = request;
			this.response = response;
		}

		@Override
		public void reportTickInProgress(TickInProgressException e) throws IOException
		{
			printBoxedErrorMessage(response, "Der Tick läuft. Bitte etwas Geduld.");
		}

		@Override
		public void reportStaleState(StaleStateException e) throws IOException
		{
			printBoxedErrorMessage(response, "Die Operation hat sich mit einer anderen überschnitten. Bitte probier es noch einmal.");
		}

		@Override
		public void reportSqlLock(GenericJDBCException e) throws IOException
		{
			printBoxedErrorMessage(response, "Die Operation hat sich mit einer anderen überschnitten. Bitte probier es noch einmal.");
		}

		@Override
		public void reportNotLoggedIn(NotLoggedInException e) throws IOException
		{
			if(!isAutomaticAccess(request))
			{
				redirectToPortal(response, "Du musst eingeloggt sein, um diese Seite zu sehen.");
			}
		}

		@Override
		public void reportInVacation(AccountInVacationModeException e) throws IOException
		{
			if(!isAutomaticAccess(request))
			{
				if(e.getDauer() > 1)
				{
					printBoxedErrorMessage(response, "Du bist noch " + e.getDauer() + " Ticks im Vacationmodus.");
				}
				else
				{
					printBoxedErrorMessage(response, "Du bist noch " + e.getDauer() + " Tick im Vacationmodus.");
				}
			}
		}

		@Override
		public void reportUnexpected(Throwable t) throws IOException
		{
			printBoxedErrorMessage(response, "Ein genereller Fehler ist aufgetreten. Die Entwickler arbeiten daran ihn zu beheben.");
		}
		
		private void redirectToPortal(ServletResponse response, String message) throws IOException
		{
			Writer sb = response.getWriter();
			sb.append("<script type=\"text/javascript\">\n");
			sb.append("var url=parent.location.href;\n");
			sb.append("parent.location.href=url.substring(0,url.indexOf('?'));");
			sb.append("</script>");
			sb.append("<div id=\"error-box\" align=\"center\">\n");
			sb.append(Common.tableBegin(430,"left"));
			sb.append("<ul>");
			sb.append("<li><span style=\"font-size:14px; color:red\">"+ message +"</span></li>\n");
			sb.append("</url>");
			sb.append(Common.tableEnd());
			sb.append("</div>\n");
		}

		private void printBoxedErrorMessage(ServletResponse response, String message) throws IOException
		{
			Writer sb = response.getWriter();
			sb.append("<div id=\"error-box\" align=\"center\">\n");
			sb.append(Common.tableBegin(430,"left"));
			sb.append("<ul>");
			sb.append("<li><span style=\"font-size:14px; color:red\">"+ message +"</span></li>\n");
			sb.append("</url>");
			sb.append(Common.tableEnd());
			sb.append("</div>\n");
		}
		
		private boolean isAutomaticAccess(ServletRequest request)
		{
			String automaticAccessParameter = request.getParameter("autoAccess");
			return automaticAccessParameter != null && automaticAccessParameter.equals("true");
		}
	}
	
	private class JsonErrorReporter implements ErrorReporter
	{
		private ServletResponse response;
		
		JsonErrorReporter(ServletRequest request, ServletResponse response)
		{
			this.response = response;
		}

		@Override
		public void reportTickInProgress(TickInProgressException e) throws IOException
		{
			JSONObject obj = JSONUtils.error("Der Tick läuft. Bitte etwas Geduld.");
			obj.getJSONObject("message")
				.accumulate("cls", e.getClass().getSimpleName());
			respondWithObject(obj);
		}

		@Override
		public void reportStaleState(StaleStateException e) throws IOException
		{
			JSONObject obj = JSONUtils.error("Die Operation hat sich mit einer anderen überschnitten. " +
					"Bitte probier es noch einmal.");
			obj.getJSONObject("message")
				.accumulate("cls", e.getClass().getSimpleName());
			respondWithObject(obj);
		}

		@Override
		public void reportSqlLock(GenericJDBCException e) throws IOException
		{
			JSONObject obj = JSONUtils.error("Die Operation hat sich mit einer anderen überschnitten. " +
					"Bitte probier es noch einmal.");
			obj.getJSONObject("message")
				.accumulate("cls", e.getClass().getSimpleName());
			respondWithObject(obj);
		}

		@Override
		public void reportNotLoggedIn(NotLoggedInException e) throws IOException
		{
			JSONObject obj = JSONUtils.error("Du musst eingeloggt sein, um diese Seite zu sehen.");
			obj.getJSONObject("message")
				.accumulate("cls", e.getClass().getSimpleName())
				.accumulate("redirect", true);
			respondWithObject(obj);
		}

		@Override
		public void reportInVacation(AccountInVacationModeException e) throws IOException
		{
			
			JSONObject obj;
			if(e.getDauer() > 1)
			{
				obj = JSONUtils.error("Du bist noch " + e.getDauer() + " Ticks im Vacationmodus.");
			}
			else
			{
				obj = JSONUtils.error("Du bist noch " + e.getDauer() + " Tick im Vacationmodus.");
			}
			obj.getJSONObject("message")
				.accumulate("cls", e.getClass().getSimpleName())
				.accumulate("redirect", true);
			respondWithObject(obj);
		}

		@Override
		public void reportUnexpected(Throwable t) throws IOException
		{
			JSONObject obj = JSONUtils.error("Ein genereller Fehler ist aufgetreten. Die Entwickler arbeiten daran ihn zu beheben.");
			obj.getJSONObject("message")
				.accumulate("cls", t.getClass().getSimpleName());
			respondWithObject(obj);
		}
		
		private void respondWithObject(JSONObject obj) throws IOException
		{
			Writer w = response.getWriter();
			w.append(obj.toString());
		}
	}
}
