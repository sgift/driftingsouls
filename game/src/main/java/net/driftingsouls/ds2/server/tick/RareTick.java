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
import net.driftingsouls.ds2.server.tick.rare.RestTick;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;

/**
 * Der seltene Tick.
 * @author Christopher Jung
 *
 */
public class RareTick extends AbstractTickExecuter
{
	private static final Log log = LogFactory.getLog(RareTick.class);
	
	@Override
	protected void executeTicks()
	{
		try
		{
			File lockFile = new File(Configuration.getLogPath()+"/raretick.lock");
			lockFile.createNewFile();
			try
			{
				publishStatus("berechne Sonstiges");
				execTick(RestTick.class, false);
			}
			finally
			{
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
			Common.mailThrowable(e, "RareTick Exception", null);
		}
	}

	@Override
	protected void prepare()
	{
		setName("");
		setLogPath(Configuration.getLogPath()+"raretick/");
	}
}
