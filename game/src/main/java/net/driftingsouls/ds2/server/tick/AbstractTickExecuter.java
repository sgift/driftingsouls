/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
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
package net.driftingsouls.ds2.server.tick;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Klasse zur Ausfuehrung von mehreren Ticks.
 *
 * @author Christopher Jung
 *
 */
public abstract class AbstractTickExecuter extends TickController
{
	private static final Log log = LogFactory.getLog(AbstractTickExecuter.class);

	private String loxpath = null;

	private String name = "";
	private String status = null;
	private Map<Class<? extends TickController>, Long> tickTimes = new LinkedHashMap<>();
	public AbstractTickExecuter()
	{
		this.loxpath = Configuration.getLogPath();
	}

	/**
	 * Setzt den Namen des Ticks. Dieser wird in LOXPATH/tix.log festgehalten.
	 *
	 * @param name Name des Ticks
	 */
	protected void setName(String name)
	{
		this.name = name;
	}

	/**
	 * Setzt den Basispfad fuer alle Logdateien. Default ist LOXPATH.
	 *
	 * @param path Basispfad fuer Logs
	 */
	protected void setLogPath(String path)
	{
		this.loxpath = path;
		if( loxpath.charAt(loxpath.length() - 1) != '/' )
		{
			loxpath += '/';
		}
		if( !new File(loxpath).isDirectory() )
		{
			log.error("Der Log-Pfad '" + loxpath + "' existiert nicht");
		}
	}

	/**
	 * Fuehrt ein Tickscript aus.
	 *
	 * @param tickname Eine Instanz des Tickscripts
	 * @param useSTDOUT Soll STDOUT oder eine Logdatei mit dem Namen des Ticks verwendet werden?
	 */
	protected void execTick(Class<? extends TickController> tickname, boolean useSTDOUT)
	{
		long start = System.currentTimeMillis();
		try
		{
			TickController tick = this.getContext().getBean(tickname, null);

			if( !useSTDOUT )
			{
				tick.addLogTarget(loxpath + tickname.getSimpleName().toLowerCase() + ".log", false);
			}
			else
			{
				tick.addLogTarget(STDOUT, false);
			}

			tick.execute();
			tick.dispose();
		}
		catch( Exception e )
		{
			// Alle Exceptions hier fangen und lediglich ausgeben
			e.printStackTrace();
		}

		this.tickTimes.put(tickname, System.currentTimeMillis()-start);
	}

	/**
	 * Kopiert alle Logs im Tick-Log-Verzeichnis in ein Unterverzeichnis mit der Tick-Nr.
	 *
	 * @param ticknr Nummer des Ticks
	 */
	protected void copyLogs(int ticknr)
	{
		log.info("Kopiere alle Logs von " + loxpath + " nach " + loxpath + ticknr + "/");
		File[] files = new File(loxpath).listFiles();
		if(files != null)
		{
			for (File file : files)
			{
				if (file.getName().endsWith(".log"))
				{
					String filename = file.getName();
					log.info("Kopiere " + filename);

					if (filename.lastIndexOf('/') > -1)
					{
						filename = filename.substring(filename.lastIndexOf('/') + 1);
					}
					Common.copyFile(file.getAbsolutePath(), loxpath + ticknr + "/" + filename);
				}
			}
		}
	}

	/**
	 * Schreibt den aktuellen Status nach ticktime.log (befindet sich unter LOXPATH und nicht im
	 * Basisverzeichnis der Tick-Logs!).
	 *
	 * @param status Der zu schreibende Status
	 */
	protected void publishStatus(String status) {
		try {
			addLogTarget(Configuration.getLogPath() + "ticktime.log", false);
			log("Bitte warten - " + status);
			removeLogTarget(Configuration.getLogPath() + "ticktime.log");

			this.status = status;
		} catch (IOException e) {
			System.err.println("Tickstatus konnte nicht publiziert werden: " + e);
		}
	}

	/**
	 * Gibt den aktuell veroeffentlichten Status des Ticks zurueck. Wenn noch kein Status gesetzt
	 * wurde, dann wird <code>null</code> zurueckgegeben
	 *
	 * @return Der aktuelle Status oder <code>null</code>
	 */
	protected String getStatus()
	{
		return this.status;
	}

	/**
	 * Vor- und Nachbereitung der Tickausfuehrung.
	 */
	@Override
	protected final void tick() {
		if (getContext().getRequest().getParameterString("only").length() > 0) {

			try {
				execTick(Class.forName(getContext().getRequest().getParameterString("only"))
						.asSubclass(TickController.class), true);
			} catch (Exception e) {
				System.err.println("Ausfuehrung des Ticks " 
						+ getContext().getRequest().getParameterString("only")
						+ " fehlgeschlagen: " + e);
				e.printStackTrace();
			}
			return;
		}

		Session db = getDB();
		Transaction transaction = db.beginTransaction();
		int ticknr = getContext().get(ContextCommon.class).getTick() + 1;
		transaction.commit();

		if (!new File(loxpath + "/" + ticknr).isDirectory()) {
			boolean result = new File(loxpath + "/" + ticknr).mkdir();
			if (!result) {
				log.error("Kann Verzeichnis '" + loxpath + "/" + ticknr + "' nicht anlegen");
			}
		}

		executeTicks();

		copyLogs(ticknr);

		try {
			addLogTarget(Configuration.getLogPath() + "tix.log", true);
			if (name.length() != 0) {
				slog(name + ": ");
			}
			addLogTarget(Configuration.getLogPath() + "ticktime.log", false);
			log(Common.date("d.m.Y H:i:s"));
			removeLogTarget(Configuration.getLogPath() + "ticktime.log");
			removeLogTarget(Configuration.getLogPath() + "tix.log");
		} catch (Exception e) {
			System.err.println("Fehler bei der Ticknachbereitung: " + e);
		}
	}

	/**
	 * Sendet die Tickstatistik, d.h. Daten ueber die Ausfuehrungsgeschwindigkeit
	 * einzelner Tickteile an die Administratoren.
	 */
	public final void mailTickStatistics()
	{
		StringBuilder stats = new StringBuilder();
		stats.append("Tick: ").append(this.getClass().getName()).append("\n\n");
		stats.append("Tickteile:\n");
		for( Map.Entry<Class<? extends TickController>, Long> entry : this.tickTimes.entrySet() )
		{
			stats.append(entry.getKey().getName()).append(": ").append(entry.getValue()).append("ms\n");
		}
		Common.sendMailToAdmins("Tickstatistik", stats.toString());
	}

	/**
	 * Fuehrt alle Einzelticks aus.
	 */
	protected abstract void executeTicks();
}
