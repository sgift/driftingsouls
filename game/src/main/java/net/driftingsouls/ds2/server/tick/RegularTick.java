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
import net.driftingsouls.ds2.server.map.StarSystemData;
import net.driftingsouls.ds2.server.tick.regular.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Der normale Tick.
 * @author Christopher Jung
 *
 */
public class RegularTick extends AbstractTickExecuter
{
	private static final Log log = LogFactory.getLog(RegularTick.class);
	
	@Override
	protected void executeTicks(List<StarSystemData> systeme)
	{
		String msg ="Starte regular tick";
		if(systeme != null && systeme.size() > 0)
		{
			msg += " fuer Systeme ["+systeme.stream().map(StarSystemData::getId).map(String::valueOf)
							.collect(Collectors.joining(","))+"]";
		}
		log(msg);
		
		try
		{
			File lockFile = new File(Configuration.getLogPath()+"/regulartick.lock");
			lockFile.createNewFile();

			try
			{
				execTick(TicksperreSetzenTick.class, false, systeme);

				log("Accounts blockiert");

				publishStatus("berechne Nutzer");
				execTick(UserTick.class, false, systeme);
				
				publishStatus("berechne Basen");
				execTick(BaseTick.class,false, systeme);
		
				publishStatus("berechne Schiffe");
				execTick(SchiffsTick.class,false, systeme);
		
				publishStatus("berechne Werften");
				execTick(WerftTick.class, false, systeme);
		
				publishStatus("berechne Forschungen");
				execTick(ForschungsTick.class, false, systeme);
		
				publishStatus("fuehre NPC-Aktionen aus");
				execTick(NPCOrderTick.class, false, systeme);
		
				publishStatus("berechne Akademien");
				execTick(AcademyTick.class, false, systeme);
				
				publishStatus("berechne Kasernen");
				execTick(KaserneTick.class, false, systeme);
		
				publishStatus("berechne GTU");
				execTick(RTCTick.class, false, systeme);

				publishStatus("berechne automatisches Feuern");
				execTick(AutofireTick.class, false, systeme);

				publishStatus("berechne Schlachten");
				execTick(BattleTick.class, false, systeme);

				publishStatus("berechne dynamische JumpNodes");
				execTick(DynJNTick.class, false, systeme);
		
				publishStatus("berechne Sonstiges");
				execTick(RestTick.class, false, systeme);
			}
			finally
			{
				execTick(TicksperreAufhebenTick.class, false, systeme);
				log("Accounts frei");

				if( !lockFile.delete() )
				{
					log.warn("Konnte Lockdatei "+lockFile+" nicht loeschen");
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
