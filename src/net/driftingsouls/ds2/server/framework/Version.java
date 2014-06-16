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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Versionsinformationen zum momentanen DS-Build. Diese Klasse extrahiert ihre Informationen
 * aus der Datei <code>/META-INF/ds.version</code>.
 * @author Christopher Jung
 *
 */
@Component
public class Version
{
	private static final Log log = LogFactory.getLog(Version.class);
	
	private String version = null;
	private String buildTime = "1970-01-01 00:00";
	private String build = "DEV";
	
	/**
	 * Konstruktor.
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
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(in)))
			{
				String line;
				while ((line = reader.readLine()) != null)
				{
					parseLine(line);
				}
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

		if( "git-version".equals(key) && value.matches("[a-zA-Z0-9]+") ) {
			version = value;
		}
		else if ( "build-time".equals(key) ) {
			buildTime = value;
		}
		else if ( "bamboo-build".equals(key) ) {
			build = value;
		}
	}
	
	/**
	 * Gibt die Versions-ID zurueck. Diese kann eine echte Commit-ID sein sofern es sich
	 * um ein regulaeres Build handelt. Fuer Entwicklungsbuilds wird keine echte Commit-ID zurueckgegeben.
	 * @return Die Versions-ID
	 * @see #isVersioned()
	 */
	public String getVersion()
	{
		return version != null ? version : "000000000000";
	}

	/**
	 * Gibt zurueck, ob eine gueltige Versionsnummer (d.h. eine gueltige Commit-ID) vorhanden ist
	 * oder ob es sich nur um ein Entwicklungsbuild handelt.
	 * @return <code>true</code>, falls eine gueltige Commit-ID vorhanden ist
	 */
	public boolean isVersioned()
	{
		return version != null;
	}
	
	/**
	 * Gibt den Zeitpunkt des Builds zurueck oder <code>null</code>, wenn der Buildzeitpunkt unbekannt ist.
	 * @return Der Zeitpunkt oder <code>null</code>
	 */
	public String getBuildTime()
	{
		return buildTime;
	}

	/**
	 * Gibt die Build-ID des Builds zurueck.
	 * @return Die Build-ID
	 */
	public String getBuild()
	{
		return build;
	}
}
