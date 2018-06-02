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

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Hilfsfunktionen fuer XML-Daten.
 * @author Christopher Jung
 *
 */
public class XMLUtils {
	private static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	private static XPathFactory xFactory = XPathFactory.newInstance();
	
	static {
		factory.setNamespaceAware(true);
	}
	
	/**
	 * Liesst eine Datei als XML-Dokument ein.
	 * @param file Der Pfad zur Datei
	 * @return Das XML-Dokument
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public static Document readFile(String file) throws SAXException, IOException, ParserConfigurationException {
		return factory.newDocumentBuilder().parse(new File(file));
	}

	/**
	 * Liesst einen Stream als XML-Dokument ein.
	 * @param stream Der Pfad zur Datei
	 * @return Das XML-Dokument
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public static Document readStream(InputStream stream) throws SAXException, IOException, ParserConfigurationException {
		return factory.newDocumentBuilder().parse(stream);
	}

	/**
	 * Schreibt das angegebene XML-Dokument in die angegebene Datei.
	 * @param file Die Zieldatei
	 * @param doc Das zu schreibene XML-Dokument
	 * @throws TransformerException Bei Fehlern waehrend des Schreibvorgangs
	 */
	public static void writeFile(String file, Document doc) throws TransformerException
	{
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(file));

		// Output to console for testing
		// StreamResult result = new StreamResult(System.out);

		transformer.transform(source, result);
	}
	
	/**
	 * Liefert eine Liste von Nodes als Ergebnis eines X-Path-Ausdrucks zurueck.
	 * @param node Der Basisknoten, von dem aus der Ausdruck ausgewertet werden soll
	 * @param xpath Der X-Path-Ausdruck
	 * @return Das Ergebnis
	 * @throws XPathExpressionException
	 */
	public static NodeList getNodesByXPath(Node node, String xpath) throws XPathExpressionException {
		XPath path = xFactory.newXPath();
		return (NodeList)path.evaluate(xpath, node, XPathConstants.NODESET);
	}
	
	/**
	 * Liefert eine Node als Ergebnis eines X-Path-Ausdrucks zurueck.
	 * @param node Der Basisknoten, von dem aus der Ausdruck ausgewertet werden soll
	 * @param xpath Der X-Path-Ausdruck
	 * @return Das Ergebnis
	 * @throws XPathExpressionException
	 */
	public static Node getNodeByXPath(Node node, String xpath) throws XPathExpressionException {
		XPath path = xFactory.newXPath();
		return (Node)path.evaluate(xpath, node, XPathConstants.NODE);
	}
	
	/**
	 * Liefert einen String als Ergebnis eines X-Path-Ausdrucks zurueck.
	 * @param node Der Basisknoten, von dem aus der Ausdruck ausgewertet werden soll
	 * @param xpath Der X-Path-Ausdruck
	 * @return Das Ergebnis
	 * @throws XPathExpressionException
	 */
	public static String getStringByXPath(Node node, String xpath) throws XPathExpressionException {
		XPath path = xFactory.newXPath();
		return (String)path.evaluate(xpath, node, XPathConstants.STRING);
	}
	
	/**
	 * Liefert eine Number als Ergebnis eines X-Path-Ausdrucks zurueck.
	 * @param node Der Basisknoten, von dem aus der Ausdruck ausgewertet werden soll
	 * @param xpath Der X-Path-Ausdruck
	 * @return Das Ergebnis
	 * @throws XPathExpressionException
	 */
	public static Number getNumberByXPath(Node node, String xpath) throws XPathExpressionException {
		XPath path = xFactory.newXPath();
		return (Number)path.evaluate(xpath, node, XPathConstants.NUMBER);
	}
	
	/**
	 * Liefert ein Booleans als Ergebnis eines X-Path-Ausdrucks zurueck.
	 * @param node Der Basisknoten, von dem aus der Ausdruck ausgewertet werden soll
	 * @param xpath Der X-Path-Ausdruck
	 * @return Das Ergebnis
	 * @throws XPathExpressionException
	 */
	public static boolean getBooleanByXPath(Node node, String xpath) throws XPathExpressionException {
		XPath path = xFactory.newXPath();
		return (Boolean)path.evaluate(xpath, node, XPathConstants.BOOLEAN);
	}
	
	/**
	 * Gibt das angegebene Attribut der Node als Boolean zurueck.
	 * Sollte das Attribut nicht existieren wird <code>false</code> zurueckgegeben.
	 * @param node Der Basisknoten, von dem aus der Ausdruck ausgewertet werden soll
	 * @param attr Der Attributname
	 * @return Das Ergebnis
	 */
	public static boolean getBooleanAttribute(Node node, String attr) {
		return "true".equalsIgnoreCase(getStringAttribute(node, attr));
	}
	
	/**
	 * Gibt das angegebene Attribut der Node als Number zurueck.
	 * Sollte das Attribut nicht existieren oder keine Zahl sein,
	 * so wird <code>null</code> zurueckgegeben.
	 * @param node Der Knoten
	 * @param attr Der Attributname
	 * @return Der Wert oder null
	 */
	public static Number getNumberAttribute(Node node, String attr) {
		if( node.getAttributes() == null ) {
			return 0;
		}
		if( node.getAttributes().getNamedItem(attr) == null ) {
			return 0;
		}
		String val = node.getAttributes().getNamedItem(attr).getNodeValue();
		if( val == null ) {
			return null;
		}
		try {
			return Long.valueOf(val);
		}
		catch( NumberFormatException e ) {
			// EMPTY
		}
		
		try {
			return Double.valueOf(val);
		}
		catch( NumberFormatException e ) {
			return null;
		}
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
	
	/**
	 * Gibt das angegebene Attribut der Node als String-Wert zurueck.
	 * Sollte das Attribut nicht existieren, so wird <code>null</code> zurueckgegeben.
	 * @param node Der Knoten
	 * @param attr Der Attributname
	 * @return Der String oder <code>null</code>
	 */
	public static String getStringAttribute(Node node, String attr) {
		if( node.getAttributes() == null ) {
			return null;
		}
		if( node.getAttributes().getNamedItem(attr) == null ) {
			return null;
		}
		return node.getAttributes().getNamedItem(attr).getNodeValue();
	}
	
	/**
	 * Gibt das erste Element mit einem bestimmten Tag-Namen unterhalb eines Knotens zurueck.
	 * Wenn kein Element mit dem Tag direkt unter dem angegebenen Knoten gefunden wird, so wird <code>null</code>
	 * zurueckgegeben.
	 *  
	 * @param node Der Knoten unter dem gesucht werden soll
	 * @param name Der Name des zu suchenden Elements
	 * @return Das erste Element mit dem Namen oder <code>null</code>
	 */
	public static Node firstNodeByTagName(Node node, String name) {
		NodeList list = node.getChildNodes();
		for( int i=0; i < list.getLength(); i++ ) {
			if( list.item(i).getNodeType() != Node.ELEMENT_NODE ) {
				continue;
			}
			if( list.item(i).getNodeName().equals(name) ) {
				return list.item(i);
			}
		}
		
		return null;
	}
	
	/**
	 * Gibt den ersten Knoten eines bestimmten Typs unterhalb eines Knotens zurueck.
	 * Wenn kein Knoten des Typs direkt unter dem angegebenen Knoten gefunden wird, so wird <code>null</code>
	 * zurueckgegeben.
	 *  
	 * @param node Der Knoten unter dem gesucht werden soll
	 * @param type Der Typ des Knotens
	 * @return Der erste Knoten vom angegebenen Typ oder <code>null</code>
	 */
	public static Node firstChildOfType(Node node, short type) {
		NodeList list = node.getChildNodes();
		for( int i=0; i < list.getLength(); i++ ) {
			if( list.item(i).getNodeType() == type) {
				return list.item(i);
			}
		}
		
		return null;
	}
}
