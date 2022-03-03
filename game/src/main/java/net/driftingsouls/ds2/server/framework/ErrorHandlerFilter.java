package net.driftingsouls.ds2.server.framework;

import com.google.gson.Gson;
import net.driftingsouls.ds2.server.framework.authentication.TickInProgressException;
import net.driftingsouls.ds2.server.user.authentication.AccountInVacationModeException;
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
	private static final Log log = LogFactory.getLog(ErrorHandlerFilter.class);

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException {
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
				reporter = new JsonErrorReporter(response);
			}
			else
			{
				reporter = new HtmlErrorReporter(response);
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
	public void init(FilterConfig arg0) {}

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

	private static class HtmlErrorReporter implements ErrorReporter
	{
		private final ServletResponse response;

		HtmlErrorReporter(ServletResponse response)
		{
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
			redirectToPortal(response);
		}

		@Override
		public void reportInVacation(AccountInVacationModeException e) throws IOException
		{
			if(e.getDauer() > 1)
			{
				printBoxedErrorMessage(response, "Du bist noch " + e.getDauer() + " Ticks im Vacation-Modus.");
			}
			else
			{
				printBoxedErrorMessage(response, "Du bist noch " + e.getDauer() + " Tick im Vacation-Modus.");
			}
		}

		@Override
		public void reportUnexpected(Throwable t) throws IOException
		{
			printBoxedErrorMessage(response, "Ein genereller Fehler ist aufgetreten. Die Entwickler arbeiten daran, ihn zu beheben.");
		}

		private void redirectToPortal(ServletResponse response) throws IOException
		{
			Writer sb = response.getWriter();
			sb.append("<script type=\"text/javascript\">\n");
			//URL holen
			sb.append("var url=parent.location.href;\n");
			sb.append("var index=url.indexOf('?');\n");
			//ist ein ? in der URL enthalten?
			sb.append("if(index>0){\n");
			//dann url bis zum Fragezeichen kuerzen
			sb.append("  url=url.substring(0,index);");
			sb.append("}\n");
			//endet sie auf 'ds'?
			sb.append("else if(url.endsWith('ds')){\n");
			//dann das 'ds' abschneiden, damit wir keine Endlosschleife haben
			sb.append("  url=url.slice(0,-2);\n");
			sb.append("}\n");
			//und jetzt zur Seite gehen
			sb.append("parent.location.href=url;");
			sb.append("</script>");
			printBoxedErrorMessage("Du musst eingeloggt sein, um diese Seite zu sehen.", sb);
		}

		private void printBoxedErrorMessage(ServletResponse response, String message) throws IOException
		{
			Writer sb = response.getWriter();
			printBoxedErrorMessage(message, sb);
		}

		private void printBoxedErrorMessage(String message, Writer sb) throws IOException {
			sb.append("<div id=\"error-box\" align=\"center\">\n");
			sb.append("<div class='gfxbox' style='width:470px'>");
			sb.append("<ul>");
			sb.append("<li><span style=\"font-size:14px; color:red\">").append(message).append("</span></li>\n");
			sb.append("</ul>");
			sb.append("</div>");
			sb.append("</div>\n");
		}
	}

	private static class JsonErrorReporter implements ErrorReporter
	{
		private final ServletResponse response;

		JsonErrorReporter(ServletResponse response)
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
				obj = ViewMessage.error("Du bist noch " + e.getDauer() + " Ticks im Vacation-Modus.");
			}
			else
			{
				obj = ViewMessage.error("Du bist noch " + e.getDauer() + " Tick im Vacation-Modus.");
			}
			obj.message.cls = e.getClass().getSimpleName();
			obj.message.redirect = true;
			respondWithObject(obj);
		}

		@Override
		public void reportUnexpected(Throwable t) throws IOException
		{
			ViewMessage obj = ViewMessage.error("Ein genereller Fehler ist aufgetreten. Die Entwickler arbeiten daran ihn, zu beheben.");
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
