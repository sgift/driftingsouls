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
package net.driftingsouls.ds2.server.framework.pipeline.reader;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletResponse;

import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.Loggable;

import org.apache.commons.io.IOUtils;

/**
 * Liesst Dateien von der Festplatte und schreibt sie in die Antwort
 * @author Christopher Jung
 *
 */
public class FileReader implements Reader, Loggable {
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
	
	private String guessMimeType( String extension ) {
		if( extension == null ) {
			return null;
		}
		if( extension.equals("html") ) {
			return "text/html";
		}
		if( extension.equals("txt") ) {
			return "text/plain";
		}
		if( extension.equals("xml") ) {
			return "text/xml";
		}
		if( extension.equals("js") ) {
			return "text/javascript";
		}
		if( extension.equals("css") ) {
			return "text/css";
		}
		if( extension.equals("png") ) {
			return "image/png";
		}
		if( extension.equals("gif") ) {
			return "image/gif";
		}
		if( extension.equals("jpg") ) {
			return "image/jpg";
		}
		
		return null;
	}
	
	public void read(String filename, Context context) throws Exception {
		/*
		 * TODO: Die Nutzung von HttpServletResponse.SC_* ist nicht gerade so elegant....
		 */
		
		// Keine Range-Unterstuetzung bis jetzt
		String range = context.getRequest().getHeader("Range");
		if( (range != null) && !"".equals(range) ) {
			context.getResponse().setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
			return;
		}
		
		String path = Configuration.getSetting("ABSOLUTE_PATH")+filename;
		File file = new File(path);
		if( !file.exists() ) {
			context.getResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
			LOG.warn("Warning: file not found: '"+file+"'");
			return;
		}
		
		int index = path.lastIndexOf('.');
		if( (index != -1) && (index < path.length()) ) {
			String type = guessMimeType(path.substring(path.lastIndexOf('.')+1));
			if( type != null ) {
				context.getResponse().setContentType(type);
			}
			else {
				String mimetype = new MimetypesFileTypeMap().getContentType(file);
				if( mimetype != null ) {
					context.getResponse().setContentType(type);
				}
			}
		}
		
		context.getResponse().setHeader("Content-Length", ""+file.length() );
		context.getResponse().setContentLength((int)file.length());
		context.getResponse().setHeader("Accept-Ranges", "none" );
		context.getResponse().setHeader("Date", dateFormat.format(new Date()));
		context.getResponse().setHeader("Last-Modified", dateFormat.format( new Date(file.lastModified()) ) );
		
		FileInputStream fin = new FileInputStream(new File(path));
		IOUtils.copy(fin, context.getResponse().getOutputStream());
	}
}
