package net.driftingsouls.ds2.server.framework;

import com.google.gson.Gson;
import net.driftingsouls.ds2.server.framework.authentication.TickInProgressException;
import net.driftingsouls.ds2.server.user.authentication.AccountInVacationModeException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.StaleStateException;
import org.hibernate.exception.GenericJDBCException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

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
				if(ex instanceof StaleStateException )
				{
					reporter.reportStaleState((StaleStateException)ex);
					return;
				}
				if(ex instanceof NotLoggedInException)
				{
					reporter.reportNotLoggedIn((NotLoggedInException)ex);
					return;
				}
				if(ex instanceof AccountInVacationModeException)
				{
					reporter.reportInVacation((AccountInVacationModeException)ex);
					return;
				}
				if( ex instanceof GenericJDBCException )
				{
					GenericJDBCException gex = (GenericJDBCException)ex;
					String msg = gex.getSQLException().getMessage();
					if( msg != null && (
							msg.startsWith("Beim Warten auf eine Sperre wurde die") ||
							msg.startsWith("Lock wait timeout exceeded")) )
					{
						reporter.reportSqlLock((GenericJDBCException) ex);
						return;
					}
				}
				ex = ex.getCause();
			}
			while(ex != null);

			String infos = erzeugeRequestInformationen((HttpServletRequest) request);

			Throwable mailThrowable = e;
			while( mailThrowable.getCause() != null ) {
				mailThrowable = mailThrowable.getCause();
			}

			Common.mailThrowable(mailThrowable, "Unexpected exception", infos);
			log.info("", e);

			reporter.reportUnexpected(e);
		}
	}

	private String erzeugeRequestInformationen(HttpServletRequest request)
	{
		StringBuilder infos = new StringBuilder(100);
		Map<String, String[]> params = request.getParameterMap();
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
		return infos.toString();
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException
	{}

	@Override
	public void destroy()
	{}
	
	private interface ErrorReporter
	{
		void reportTickInProgress(TickInProgressException e) throws IOException;
		void reportStaleState(StaleStateException e) throws IOException;
		void reportSqlLock(GenericJDBCException e) throws IOException;
		void reportNotLoggedIn(NotLoggedInException e) throws IOException;
		void reportInVacation(AccountInVacationModeException e) throws IOException;
		void reportUnexpected(Throwable t) throws IOException;
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
			redirectToPortal(response, "Du musst eingeloggt sein, um diese Seite zu sehen.");
		}

		@Override
		public void reportInVacation(AccountInVacationModeException e) throws IOException
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
			sb.append("<div class='gfxbox' style='width:470px'>");
			sb.append("<ul>");
			sb.append("<li><span style=\"font-size:14px; color:red\">").append(message).append("</span></li>\n");
			sb.append("</ul>");
			sb.append("</div>");
			sb.append("</div>\n");
		}

		private void printBoxedErrorMessage(ServletResponse response, String message) throws IOException
		{
			Writer sb = response.getWriter();
			sb.append("<div id=\"error-box\" align=\"center\">\n");
			sb.append("<div class='gfxbox' style='width:470px'>");
			sb.append("<ul>");
			sb.append("<li><span style=\"font-size:14px; color:red\">").append(message).append("</span></li>\n");
			sb.append("</ul>");
			sb.append("</div>");
			sb.append("</div>\n");
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
			ViewMessage obj = ViewMessage.error("Der Tick läuft. Bitte etwas Geduld.");
			obj.message.cls = e.getClass().getSimpleName();
			respondWithObject(obj);
		}

		@Override
		public void reportStaleState(StaleStateException e) throws IOException
		{
			ViewMessage obj = ViewMessage.error("Die Operation hat sich mit einer anderen überschnitten. " +
					"Bitte probier es noch einmal.");
			obj.message.cls = e.getClass().getSimpleName();
			respondWithObject(obj);
		}

		@Override
		public void reportSqlLock(GenericJDBCException e) throws IOException
		{
			ViewMessage obj = ViewMessage.error("Die Operation hat sich mit einer anderen überschnitten. " +
					"Bitte probier es noch einmal.");
			obj.message.cls = e.getClass().getSimpleName();
			respondWithObject(obj);
		}

		@Override
		public void reportNotLoggedIn(NotLoggedInException e) throws IOException
		{
			ViewMessage obj = ViewMessage.error("Du musst eingeloggt sein, um diese Seite zu sehen.");
			obj.message.cls = e.getClass().getSimpleName();
			obj.message.redirect = true;
			respondWithObject(obj);
		}

		@Override
		public void reportInVacation(AccountInVacationModeException e) throws IOException
		{

			ViewMessage obj;
			if(e.getDauer() > 1)
			{
				obj = ViewMessage.error("Du bist noch " + e.getDauer() + " Ticks im Vacationmodus.");
			}
			else
			{
				obj = ViewMessage.error("Du bist noch " + e.getDauer() + " Tick im Vacationmodus.");
			}
			obj.message.cls = e.getClass().getSimpleName();
			obj.message.redirect = true;
			respondWithObject(obj);
		}

		@Override
		public void reportUnexpected(Throwable t) throws IOException
		{
			ViewMessage obj = ViewMessage.error("Ein genereller Fehler ist aufgetreten. Die Entwickler arbeiten daran ihn zu beheben.");
			obj.message.cls = t.getClass().getSimpleName();
			respondWithObject(obj);
		}
		
		private void respondWithObject(ViewMessage obj) throws IOException
		{
			Writer w = response.getWriter();
			w.append(new Gson().toJson(obj));
		}
	}
}
