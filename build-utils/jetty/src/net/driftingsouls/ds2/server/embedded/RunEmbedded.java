package net.driftingsouls.ds2.server.embedded;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Starter fuer Jetty mit integrierter DS-Version.
 * @author christopherjung
 *
 */
public class RunEmbedded {

	/**
	 * Main.
	 * <p>Es wird genau ein Argument unterstuetzt, der Pfad zur DS-Webapp (Verzeichnis oberhalb
	 * von WEB-INF).
	 * @param args Die Argumente
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String dsPort = System.getProperty("DS_PORT");
		Server server = new Server(dsPort != null ? Integer.parseInt(dsPort) : 8080);

		WebAppContext webapp = new WebAppContext();
		webapp.setContextPath("/driftingsouls");
		webapp.setWar(args[0]);
		webapp.setParentLoaderPriority(true);
		server.setHandler(webapp);

		server.start();
		server.join();
	}

}
