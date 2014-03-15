package net.driftingsouls.ds2.server.framework.db;

import net.driftingsouls.ds2.server.framework.DSFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;
import org.hibernate.context.internal.ManagedSessionContext;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * Ein Filter, um eine neue Session zu oeffnen fuer den Request.
 * Implementiert die SessionInView- und SessionPerRequest-Patterns von Hibernate.
 *
 * @author Drifting-Souls Team
 */
public class HibernateSessionRequestFilter extends DSFilter
{
	@Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        if(!isStaticRequest(request))
        {
			Session session = sf.openSession();
			try
            {
				ManagedSessionContext.bind(session);

                log.debug("Starting a database transaction");
				session.beginTransaction();

                // Call the next filter (continue request processing)
                chain.doFilter(request, response);

                // Commit and cleanup
                log.debug("Committing the database transaction");

				ManagedSessionContext.unbind(sf);

				if( session.getTransaction().isActive() )
				{
					session.getTransaction().commit();
				}
            }
            catch (StaleObjectStateException staleEx)
            {
				ManagedSessionContext.unbind(sf);
				if (session.getTransaction().isActive())
				{
					session.getTransaction().rollback();
				}

                log.error("This interceptor does not implement optimistic concurrency control!");
                log.error("Your application will not work until you add compensation actions!");
                // Rollback, close everything, possibly compensate for any permanent changes
                // during the conversation, and finally restart business conversation. Maybe
                // give the user of the application a chance to merge some of his work with
                // fresh data... what you do here depends on your applications design.
                throw staleEx;
            }
            catch (Throwable ex)
            {
				ManagedSessionContext.unbind(sf);
                try
                {
                    if (session.getTransaction().isActive())
                    {
                        log.debug("Trying to rollback database transaction after exception");
						session.getTransaction().rollback();
                    }
                }
                catch (Throwable rbEx)
                {
                    log.error("Could not rollback transaction after exception!", rbEx);
                }

                throw new ServletException(ex);
            }
			finally
			{
				session.close();
			}
		}
        else
        {
            chain.doFilter(request, response);
        }
    }

	@Override
	protected void initFilterBean() throws ServletException
	{
		log.debug("Initializing filter...");
		log.debug("Obtaining SessionFactory from static HibernateUtil singleton");
		sf = HibernateUtil.getSessionFactory();
	}

	@Override
    public void destroy() {}

    private static Log log = LogFactory.getLog(HibernateSessionRequestFilter.class);
    private SessionFactory sf;
}