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
package net.driftingsouls.ds2.server.framework.xml;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Hilfsfunktionen fuer XML-Daten
 * @author Christopher Jung
 *
 */
public class XMLUtils {
	private static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	private static XPathFactory xFactory = XPathFactory.newInstance();
	
	public static Document readFile(String file) throws SAXException, IOException, ParserConfigurationException {
		return factory.newDocumentBuilder().parse(new File(file));
	}
	
	public static NodeList getNodesByXPath(Node node, String xpath) throws XPathExpressionException {
		XPath path = xFactory.newXPath();
		return (NodeList)path.evaluate(xpath, node, XPathConstants.NODESET);
	}
	
	public static Node getNodeByXPath(Node node, String xpath) throws XPathExpressionException {
		XPath path = xFactory.newXPath();
		return (Node)path.evaluate(xpath, node, XPathConstants.NODE);
	}
	
	public static String getStringByXPath(Node node, String xpath) throws XPathExpressionException {
		XPath path = xFactory.newXPath();
		return (String)path.evaluate(xpath, node, XPathConstants.STRING);
	}
	
	public static Number getNumberByXPath(Node node, String xpath) throws XPathExpressionException {
		XPath path = xFactory.newXPath();
		return (Number)path.evaluate(xpath, node, XPathConstants.NUMBER);
	}
	
	public static boolean getBooleanByXPath(Node node, String xpath) throws XPathExpressionException {
		XPath path = xFactory.newXPath();
		return (Boolean)path.evaluate(xpath, node, XPathConstants.BOOLEAN);
	}
	
	/**
	 * Gibt das angegebene Attribut der Node als Long-Wert zurueck.
	 * Sollte das Attribut nicht existieren oder keine Zahl sein,
	 * so wird 0 zurueckgegeben.
	 * @param node Der Knoten
	 * @param attr Der Attributname
	 * @return Der Wert oder 0
	 */
	public static long getLongAttribute(Node node, String attr) {
		if( node.getAttributes() == null ) {
			return 0;
		}
		if( node.getAttributes().getNamedItem(attr) == null ) {
			return 0;
		}
		String val = node.getAttributes().getNamedItem(attr).getNodeValue();
		if( val == null ) {
			return 0;
		}
		try {
			return Long.parseLong(val);
		}
		catch( NumberFormatException e ) {
			return 0;
		}
	}
}
