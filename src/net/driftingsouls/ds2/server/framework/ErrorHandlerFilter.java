package net.driftingsouls.ds2.server.framework;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import net.driftingsouls.ds2.server.framework.authentication.TickInProgressException;
import net.driftingsouls.ds2.server.user.authentication.AccountInVacationModeException;

import org.hibernate.StaleObjectStateException;
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
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException 
	{
		try
		{
			chain.doFilter(request, response);
		}
		catch(Exception e)
		{
			Throwable ex = e;
			do
			{
				if(ex instanceof TickInProgressException)
				{
					printBoxedErrorMessage(response, "Der Tick l&auml;uft. Bitte etwas Geduld.");
					return;
				}
				else if(ex instanceof StaleStateException || ex instanceof StaleObjectStateException)
				{
					printBoxedErrorMessage(response, "Die Operation hat sich mit einer anderen &uumlberschnitten. Bitte probier es noch einmal.");
					return;
				}
				else if((ex instanceof GenericJDBCException) && (((GenericJDBCException)ex).getSQLException().getMessage() != null) && ((GenericJDBCException)ex).getSQLException().getMessage().startsWith("Beim Warten auf eine Sperre wurde die") ) 
				{
					printBoxedErrorMessage(response, "Die Operation hat sich mit einer anderen &uumlberschnitten. Bitte probier es noch einmal.");
					return;
				}
				else if(ex instanceof NotLoggedInException)
				{
					if(!isAutomaticAccess(request))
					{
						printBoxedErrorMessage(response, "Du musst eingeloggt sein, um diese Seite zu sehen.");
					}
					return;
				}
				else if(ex instanceof AccountInVacationModeException)
				{
					AccountInVacationModeException vacException = (AccountInVacationModeException)e.getCause();
					if(!isAutomaticAccess(request))
					{
						if(vacException.getDauer() > 1)
						{
							printBoxedErrorMessage(response, "Du bist noch " + vacException.getDauer() + " Ticks im Vacationmodus.");
						}
						else
						{
							printBoxedErrorMessage(response, "Du bist noch " + vacException.getDauer() + " Tick im Vacationmodus.");
						}
					}
					return;
				}
				ex = ex.getCause();
			}
			while(ex != null);
			
			printBoxedErrorMessage(response, "Ein genereller Fehler ist aufgetreten. Die Entwickler arbeiten daran ihn zu beheben.");
			Common.mailThrowable(e, "Unexpected exception", "");
			e.printStackTrace();
		}
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException 
	{}

	@Override
	public void destroy() 
	{}
	
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
		if(automaticAccessParameter != null && automaticAccessParameter.equals("true"))
		{
			return true;
		}
		
		return false;
	}
}
