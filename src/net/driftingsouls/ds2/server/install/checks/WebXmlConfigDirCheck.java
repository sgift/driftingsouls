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
package net.driftingsouls.ds2.server.install.checks;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

/**
 * Ueberprueft, ob die config.xml geparset werden kann.
 * @author Christopher Jung
 *
 */
public class WebXmlConfigDirCheck implements Checkable {
	@Override
	public void doCheck() throws CheckFailedException {
		try {
			XPathFactory xFactory = XPathFactory.newInstance();
			
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.parse(new File("WEB-INF/web.xml"));
			
			XPath path = xFactory.newXPath();
			String configdir = (String)path.evaluate(
					"/web-app/context-param[param-name/text()='configdir']/param-value/text()", 
					doc, XPathConstants.STRING);
			
			if( configdir == null ) {
				throw new CheckFailedException("WEB-INF/web.xml hat keinen context-param 'configdir'");
			}
			
			if( !configdir.endsWith("/") ) {
				throw new CheckFailedException("configdir endet nicht auf '/'");
			}
			
			if( !new File(configdir).isDirectory() ) {
				throw new CheckFailedException("configdir ist kein Verzeichnis");
			}
			
			if( !new File(configdir+"config.xml").isFile() ) {
				throw new CheckFailedException("configdir zeigt auf kein Configverzeichnis");
			}
		}
		catch( Exception e ) {
			if( e instanceof CheckFailedException ) {
				throw (CheckFailedException)e;
			}
			throw new CheckFailedException("WEB-INF/web.xml ist kein xml", e);
		}
	}

	@Override
	public String getDescription() {
		return "web.xml: configdir pruefen";
	}

}
