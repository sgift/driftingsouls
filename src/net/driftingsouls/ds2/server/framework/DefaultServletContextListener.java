/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.driftingsouls.ds2.server.framework;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import net.driftingsouls.ds2.server.framework.db.HibernateFacade;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;

/**
 * Der Default-Listener fuer ServletContext-Events. Laedt alle fuer DS notwendigen Daten und
 * gibt diese am Ende (sofern notwendig) wieder frei.
 * @author Christopher Jung
 *
 */
public class DefaultServletContextListener implements ServletContextListener {
	private Log LOG = null;
	
	public void contextDestroyed(ServletContextEvent event) {
		HibernateFacade.free();
	}

	public void contextInitialized(ServletContextEvent event) {
		ServletContext context = event.getServletContext();

		if( context.getInitParameter("logger") == null ) {
			System.getProperties().setProperty("org.apache.commons.logging.Log","org.apache.commons.logging.impl.SimpleLog");
		}
		else {
			System.getProperties().setProperty("org.apache.commons.logging.Log",context.getInitParameter("logger"));
		}
		
		LOG = new LogFactoryImpl().getInstance("DS2");
		LOG.info("Booting DS...");
		
		try {
			new DriftingSouls(LOG, context.getInitParameter("configdir"), true);
		}
		catch( Throwable e ) {
			LOG.fatal(e, e);
			throw new RuntimeException(e);
		}
		
		LOG.info("DS is now ready for service");
	}
}
