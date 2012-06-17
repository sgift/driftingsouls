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

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.fileupload.FileItem;

/**
 * Verwaltungsklasse fuer durch Spieler hochgeladene Dateien. 
 * @author Christopher Jung
 *
 */
public class DynamicContentManager
{
	private static final long MAX_UPLOAD_SIZE= 1024*1024*2;
	
	/**
	 * Speichert die angegebene Datei auf dem Server ab und gibt die ID der Datei
	 * zurueck.
	 * @param item Die zu speichernde Datei
	 * @return Die ID
	 * @throws IOException
	 */
	public static String add(FileItem item) throws IOException
	{
		if( item.getSize() > MAX_UPLOAD_SIZE )
		{
			throw new IOException("Datei zu gross");
		}
		
		String id = UUID.randomUUID().toString();
		String suffix = "";
		if( "image/gif".equals(item.getContentType()) )
		{
			suffix = ".gif";
		}
		else if( "image/png".equals(item.getContentType()) )
		{
			suffix = ".png";
		}
		else if( item.getName() != null && item.getName().indexOf('.') > -1 )
		{
			suffix = item.getName().substring(item.getName().lastIndexOf('.'));
		}
		
		String uploaddir = Configuration.getSetting("ABSOLUTE_PATH")+"data/dynamicContent/";
		try {
			File uploadedFile = new File(uploaddir+id+suffix);
			item.write(uploadedFile);
		}
		catch( Exception e ) {
			throw new IOException(e);
		}
		return id+suffix;
	}
	
	/**
	 * Entfernt eine Datei vom Server.
	 * @param id Die ID der Datei
	 */
	public static void remove(String id)
	{
		String uploaddir = Configuration.getSetting("ABSOLUTE_PATH")+"data/dynamicContent/";
		new File(uploaddir+id).delete();
	}
}
