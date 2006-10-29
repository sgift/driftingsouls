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

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class SimpleXMLConsumer implements XMLPipe {	
	private XMLConsumer consumer;

	public void setDocumentLocator(Locator locator) {
		consumer.setDocumentLocator(locator);
	}

	public void startDocument() throws SAXException {
		consumer.startDocument();
	}

	public void endDocument() throws SAXException {
		consumer.endDocument();
	}

	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		consumer.startPrefixMapping(prefix, uri);
	}

	public void endPrefixMapping(String prefix) throws SAXException {
		consumer.endPrefixMapping(prefix);
	}

	public void startElement(String uri, String localName, String qName, Attributes atts)
			throws SAXException {
		consumer.startElement(uri, localName, qName, atts);
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		consumer.endElement(uri, localName, qName);
	}

	public void characters(char[] ch, int start, int length) throws SAXException {
		consumer.characters(ch, start, length);
	}

	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		consumer.ignorableWhitespace(ch, start, length);
	}

	public void processingInstruction(String target, String data) throws SAXException {
		consumer.processingInstruction(target, data);
	}

	public void skippedEntity(String name) throws SAXException {
		consumer.skippedEntity(name);
	}

	public void startDTD(String name, String publicId, String systemId) throws SAXException {
		consumer.startDTD(name, publicId, systemId);
	}

	public void endDTD() throws SAXException {
		consumer.endDTD();
	}

	public void startEntity(String name) throws SAXException {
		consumer.startEntity(name);
	}

	public void endEntity(String name) throws SAXException {
		consumer.endEntity(name);
	}

	public void startCDATA() throws SAXException {
		consumer.startCDATA();
	}

	public void endCDATA() throws SAXException {
		consumer.endCDATA();
	}

	public void comment(char[] ch, int start, int length) throws SAXException {
		consumer.comment(ch, start, length);
	}

	public void setConsumer(XMLConsumer consumer) {
		this.consumer = consumer;
	}

	public void characters(String characters) throws SAXException {
		consumer.characters(characters.toCharArray(), 0, characters.length());
	}

	public void endElement(String name) throws SAXException {
		consumer.endElement("", name, name);
	}

	public void startElement(String name, Attributes attributes) throws SAXException {
		consumer.startElement("", name, name, attributes);
	}

	public void comment(String comment) throws SAXException {
		consumer.comment(comment.toCharArray(), 0, comment.length());
	}
}
