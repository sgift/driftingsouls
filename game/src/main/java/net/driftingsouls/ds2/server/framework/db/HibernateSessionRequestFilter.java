package net.driftingsouls.ds2.server.framework.db;

import net.driftingsouls.ds2.server.framework.DSFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.StaleObjectStateException;

import javax.persistence.EntityManager;
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
	private static Log log = LogFactory.getLog(HibernateSessionRequestFilter.class);

	@Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        if(!isStaticRequest(request))
        {
			EntityManager em = HibernateUtil.getCurrentEntityManager();
			try
            {
                log.debug("Starting a database transaction");
				em.getTransaction().begin();

                // Call the next filter (continue request processing)
                chain.doFilter(request, response);

                // Commit and cleanup
                log.debug("Committing the database transaction");

				if( em.getTransaction().isActive() )
				{
					em.getTransaction().commit();
				}
            }
            catch (StaleObjectStateException staleEx)
            {
				if (em.getTransaction().isActive())
				{
					em.getTransaction().rollback();
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
			    try
                {
                    if (em.getTransaction().isActive())
                    {
                        log.debug("Trying to rollback database transaction after exception");
						em.getTransaction().rollback();
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
				HibernateUtil.removeCurrentEntityManager();
				em.close();
			}
		}
        else
        {
            chain.doFilter(request, response);
        }
    }

	@Override
    public void destroy() {}
}