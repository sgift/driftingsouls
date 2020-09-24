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

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.tick.regular.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Der normale Tick.
 * @author Christopher Jung
 *
 */
@Component
public class RegularTick extends AbstractTickExecutor
{
	private static final Log log = LogFactory.getLog(RegularTick.class);

	@Override
	@Scheduled(cron = "0 0 0,8,12,16,18,20,22 * * *")
	public void execute() {
		super.execute();
	}
	
	@Override
	protected void executeTicks()
	{
		log("Starte regular tick.");
		
		try
		{
			File lockFile = new File(Configuration.getLogPath()+"/regulartick.lock");
			if(!lockFile.createNewFile()) {
				log.error("Konnte LockFile nicht anlegen -> Tick abgebrochen");
			}

			try
			{
				execTick(TicksperreSetzenTick.class, false);

				log("Accounts blockiert");

				publishStatus("berechne Nutzer");
				execTick(UserTick.class, false);
				
				publishStatus("berechne Basen");
				execTick(BaseTick.class,false);
		
				publishStatus("berechne Schiffe");
				execTick(SchiffsTick.class,false);
		
				publishStatus("berechne Werften");
				execTick(WerftTick.class, false);
		
				publishStatus("berechne Forschungen");
				execTick(ForschungsTick.class, false);
		
				publishStatus("fuehre NPC-Aktionen aus");
				execTick(NPCOrderTick.class, false);
		
				publishStatus("berechne Akademien");
				execTick(AcademyTick.class, false);
				
				publishStatus("berechne Kasernen");
				execTick(KaserneTick.class, false);
		
				publishStatus("berechne GTU");
				execTick(RTCTick.class, false);

                publishStatus("berechne automatisches Feuern");
                execTick(AutofireTick.class, false);
		
				publishStatus("berechne Schlachten");
				execTick(BattleTick.class, false);

                publishStatus("berechne dynamische JumpNodes");
                execTick(DynJNTick.class, false);
		
				publishStatus("berechne Sonstiges");
				execTick(RestTick.class, false);
			}
			finally
			{
				execTick(TicksperreAufhebenTick.class, false);
				log("Accounts frei");

				try {
					Files.delete(lockFile.toPath());
				} catch(IOException ex) {
					log.error("Unable to delete lockFile", ex);
				}
			}
			
			this.mailTickStatistics();
		}
		catch( Throwable e )
		{
			log.error("Fehler beim Ausfuehren der Ticks", e);
			Common.mailThrowable(e, "RegularTick Exception", null);
		}
		
		log("Regulartick beendet");
	}

	@Override
	protected void prepare()
	{
		setName("");
		setLogPath(Configuration.getLogPath()+"tick/");
	}
}
