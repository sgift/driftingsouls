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

import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.ReaderPipeline;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Liesst Dateien von der Festplatte und schreibt sie in die Antwort.
 * @author Christopher Jung
 *
 */
public class FileReader implements Reader {
	private static final Log log = LogFactory.getLog(FileReader.class);

	private Map<String,String> extensionMap;

	/**
	 * Konstruktor.
	 */
	public FileReader()
	{
		this.extensionMap = new HashMap<>();
		this.extensionMap.put("html", "text/html");
		this.extensionMap.put("txt", "text/plain");
		this.extensionMap.put("xml", "text/xml");
		this.extensionMap.put("js", "text/javascript");
		this.extensionMap.put("css", "text/css");
		this.extensionMap.put("png", "image/png");
		this.extensionMap.put("gif", "image/gif");
		this.extensionMap.put("jpg", "image/jpg");
		this.extensionMap.put("svg", "image/svg+xml");
	}

	private String guessMimeType( String extension ) {
		if( extension == null ) {
			return null;
		}
		return this.extensionMap.get(extension);
	}
	
	@Override
	public void read(Context context, ReaderPipeline pipeline) throws Exception {
		String filename = pipeline.getFile();
		
		/*
		 * TODO: Die Nutzung von HttpServletResponse.SC_* ist nicht gerade so elegant....
		 */
		
		// Keine Range-Unterstuetzung bis jetzt
		String range = context.getRequest().getHeader("Range");
		if( (range != null) && !"".equals(range) ) {
			context.getResponse().setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
			context.getResponse().getWriter().append("416");
			
			return;
		}
		
		String path = Configuration.getAbsolutePath()+filename;
		File file = new File(path);
		if( !file.exists() ) {
			context.getResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
			context.getResponse().getWriter().append("404 - Die von ihnen gesuchte Datei existiert nicht");
			log.warn("Warning: file not found: '"+file+"'");
			
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
					context.getResponse().setContentType(mimetype);
				}
			}
		}
		
		final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
		
		context.getResponse().setHeader("Content-Length", ""+file.length() );
		context.getResponse().setContentLength((int)file.length());
		context.getResponse().setHeader("Accept-Ranges", "none" );
		context.getResponse().setHeader("Date", dateFormat.format(new Date()));
		context.getResponse().setHeader("Last-Modified", dateFormat.format( new Date(file.lastModified()) ) );

		try (FileInputStream fin = new FileInputStream(new File(path)))
		{
			IOUtils.copy(fin, context.getResponse().getOutputStream());
		}
		catch (IOException e)
		{
			// Ignorieren, da es sich in den meisten Faellen um einen Browser handelt,
			// der die Verbindung zu frueh dicht gemacht hat
		}
	}
}
