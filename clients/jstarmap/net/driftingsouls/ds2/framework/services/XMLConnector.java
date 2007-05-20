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

package net.driftingsouls.ds2.framework.services;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * XMLConnector ermoeglicht es URLs vom DS-Server als XML abzurufen
 * 
 * @author Christopher Jung
 *
 */
public class XMLConnector implements ServerConnectable {
	/**
	 * Der Service-Name
	 */
	public static final String SERVICE = XMLConnector.class.getName();
	
	/**
	 * Ruft eine DS-URL auf und versucht eine XML-Ausgabe zu erhalten. Es existiert aber nicht zu jeder URL
	 * eine solche XML-Variante!
	 * 
	 * @param module Das aufzurufende Modul
	 * @param action Die aufzurufende Action
	 * @param params Weitere Parameter (führendes & nicht vergessen!)
	 * @return Das XML-Dokument
	 * @throws XMLConnectorException
	 */
	public Document fetchStyledURL( String module, String action, String params ) throws XMLConnectorException {
		return fetchXMLUrl( "ds?module="+module+"&action="+action+"&sess="+ServerConnector.getInstance().getSession()+"&_style=xml"+params );
	}
	
	/**
	 * Ruft eine DS-URL auf und gibt sie als XML-Dokument zurueck. Anders als bei {@link #fetchStyledURL(String, String, String)}
	 * wird nicht versucht eine alternative XML-Ausgabe zu bekommen
	 * 
	 * @param module Das aufzurufende Modul
	 * @param action Die aufzurufende Action
	 * @param params Weitere Parameter (führendes & nicht vergessen!)
	 * @return Das XML-Dokument
	 * @throws XMLConnectorException
	 */
	public Document fetchURL( String module, String action, String params ) throws XMLConnectorException {
		return fetchXMLUrl( "ds?module="+module+"&action="+action+"&sess="+ServerConnector.getInstance().getSession()+params );
	}
	
	private Document fetchXMLUrl( String addurl ) throws XMLConnectorException  {	
		Document doc = null;
		System.out.println("FETCHING: "+ServerConnector.getInstance().getServerURL()+addurl);
		
		try {
			URL url = new URL(ServerConnector.getInstance().getServerURL()+addurl);
			URLConnection connection = url.openConnection();
			HttpURLConnection httpConn = (HttpURLConnection) connection;

			// Verbindung oeffnen und HTTP-Anfrage senden....
			httpConn.setRequestProperty( "Content-Length", "0" );
			httpConn.setRequestProperty( "User-Agent", "DS2.JStarmap" );
			httpConn.setRequestProperty( "Content-Type","text/html; charset=utf-8" );
			httpConn.setDoInput(true);

			// ....und nun lesen wir mal ein wenig :)

			InputStream in = httpConn.getInputStream();
			InputSource ins = new InputSource(in);
		
		
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder parser =  factory.newDocumentBuilder();
		
			doc = parser.parse(ins);
		
			in.close();
			httpConn.disconnect();
		
		}
		catch(Exception e) {
			throw new XMLConnectorException(e);
		}		
		
		return doc;
	}
}
