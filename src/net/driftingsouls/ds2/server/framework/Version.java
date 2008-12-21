/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Versionsinformationen zum momentanen DS-Build. Diese Klasse extrahiert ihre Informationen
 * aus der Datei <code>/META-INF/ds.version</code>.
 * @author Christopher Jung
 *
 */
public class Version
{
	private static final Log log = LogFactory.getLog(Version.class);
	
	private String HG_VERSION = null;
	private String BUILD_TIME = null;
	
	/**
	 * Konstruktor
	 */
	public Version() {
		parseVersionFile();
	}

	private void parseVersionFile()
	{
		InputStream in = Version.class.getResourceAsStream("/META-INF/ds.version");
		if( in == null )
		{
			return;
		}
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			try
			{
				String line;
				while( (line = reader.readLine()) != null )
				{
					parseLine(line);
				}
			}
			finally
			{
				reader.close();
			}
		}
		catch( IOException e )
		{
			log.error("Konnte Versionsdatei nicht parsen", e);
		}
	}

	private void parseLine(String line)
	{
		int index = line.indexOf('=');
		if( index == -1 || index == line.length()-1 )
		{
			return;
		}
		String key = line.substring(0, index);
		String value = line.substring(index+1,line.length());
		
		if( "hg-version".equals(key) ) {
			HG_VERSION = value;
		}
		else if ( "build-time".equals(key) ) {
			BUILD_TIME = value;
		}
	}
	
	/**
	 * Gibt die Mercurial-Versionsid zurueck. Wenn keine Versionsid bekannt ist wird <code>null</code>
	 * zurueckgegeben. 
	 * @return Die Versionsid oder <code>null</code>
	 */
	public String getHgVersion()
	{
		return HG_VERSION;
	}
	
	/**
	 * Gibt den Zeitpunkt des Builds zurueck oder <code>null</code>, wenn der Buildzeitpunkt unbekannt ist.
	 * @return Der Zeitpunkt oder <code>null</code>
	 */
	public String getBuildTime()
	{
		return BUILD_TIME;
	}
}
