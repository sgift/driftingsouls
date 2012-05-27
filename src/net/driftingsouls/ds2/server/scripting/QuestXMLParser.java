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
package net.driftingsouls.ds2.server.scripting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.DSObject;
import net.driftingsouls.ds2.server.framework.db.Database;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Ermoeglicht das Verarbeiten und Installieren von Quest-XMLs.
 * ACHTUNG: Der nun folgende Code ist bestenfalls suboptimal, graunenvoll zu verstehen und vollstaendig
 * undokumentiert. 
 * Daher gilt: Zu Risiken und Nebenwirkungen fragen sie bitte ihren Arzt oder Apotheker (nur nicht mich).
 * @author Christopher Jung
 *
 */
@Configurable
public class QuestXMLParser extends DSObject {
	/**
	 * Der Modus in dem der Parser betrieben wird.
	 *
	 */
	public enum Mode {
		/**
		 * Liesst Installationsdaten falls das Quest bereits installiert wurde.
		 */
		READ,
		/**
		 * Installiert ein Quest.
		 */
		INSTALL
	}
	
	private static class ScriptEntry {
		int id;
		List<String> script;
		Map<String,Integer> injectindex;
		
		ScriptEntry() {
			this.script = new ArrayList<String>();
			this.injectindex = new HashMap<String,Integer>();
		}
		
		ScriptEntry( int id ) {
			this();
			this.id = id;
		}
		
		ScriptEntry( int id, List<String> script, Map<String,Integer> injectindex ) {
			this(id);
			this.script = script;
			this.injectindex = injectindex;
		}
	}
	
	protected Map<String,Integer> questids = new HashMap<String,Integer>();
	protected Map<String,Integer> answerids = new HashMap<String,Integer>();
	protected Map<String,Integer> dialogids = new HashMap<String,Integer>();
	protected Map<String,ScriptEntry> scripts = new HashMap<String,ScriptEntry>();
	
	protected List<String> addParseFiles = new ArrayList<String>();
	
	protected Map<String,String> installFiles = new HashMap<String,String>();
	
	protected Set<String> compiledFiles = new HashSet<String>();
	
	private String currentFile;
	
	private Configuration config;
	
    /**
     * Injiziert die DS-Konfiguration.
     * @param config Die DS-Konfiguration
     */
    @Autowired
    public void setConfiguration(Configuration config) 
    {
    	this.config = config;
    }
	
	/**
	 * Konstruktor.
	 * @param mode Der Modus in dem eine Quest-XML verarbeitet werden soll
	 * @param file Der Pfad zur Quest-XML
	 */
	public QuestXMLParser( Mode mode, String file ) {
		if( mode == Mode.READ ) {
			loadInstallData(file);
		}
		else {
			parsexml(file);
		}
	}
	
	private class InstallParser implements ContentHandler {
		private Database db;
		private Set<String> validTags = new HashSet<String>();
		private boolean doNotUninstall;
		private String currentFile;
		
		/**
		 * Konstruktor.
		 * @param currentFile Der Name der mit diesen Parser uebersetzten Datei
		 * @param doNotUninstall Soll das install-xml nur geparst, nicht aber ausgefuehrt werden (<code>true</code>)?
		 */
		public InstallParser(String currentFile, boolean doNotUninstall) {
			this.db = ContextMap.getContext().getDatabase();
			
			this.doNotUninstall = doNotUninstall;
			this.currentFile = currentFile;
			
			this.validTags.add("install");
			this.validTags.add("answerid");
			this.validTags.add("questid");
			this.validTags.add("dialogid");
			this.validTags.add("scriptid");
			this.validTags.add("depend");
			this.validTags.add("usedby");
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			// EMPTY
		}

		@Override
		public void endDocument() throws SAXException {
			// EMPTY
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			// EMPTY
		}

		@Override
		public void endPrefixMapping(String prefix) throws SAXException {
			// EMPTY
		}

		@Override
		public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
			// EMPTY
		}

		@Override
		public void processingInstruction(String target, String data) throws SAXException {
			// EMPTY
		}

		@Override
		public void setDocumentLocator(Locator locator) {
			// EMPTY
		}

		@Override
		public void skippedEntity(String name) throws SAXException {
			// EMPTY
		}

		@Override
		public void startDocument() throws SAXException {
			// EMPTY
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
			if( this.validTags.contains(localName.toLowerCase()) ) {
				String tag = localName.toLowerCase();
				
				if( tag.equals("questid") ) {
					questids.put(this.currentFile+":"+atts.getValue("id"), Integer.parseInt(atts.getValue("db")));
					
					if( !this.doNotUninstall ) {
						db.update("DELETE FROM quests WHERE id="+Integer.parseInt(atts.getValue("db")));
					}
				}
				else if( tag.equals("answerid") ) {
					answerids.put(this.currentFile+":"+atts.getValue("id"), Integer.parseInt(atts.getValue("db")));
					
					if( !this.doNotUninstall ) {
						db.update("DELETE FROM quests_answers WHERE id="+Integer.parseInt(atts.getValue("db")));
					}
				}
				else if( tag.equals("dialogid") ) {
					dialogids.put(this.currentFile+":"+atts.getValue("id"), Integer.parseInt(atts.getValue("db")));
					
					if( !this.doNotUninstall ) {
						db.update("DELETE FROM quests_text WHERE id="+Integer.parseInt(atts.getValue("db")));
					}
				}
				else if( tag.equals("scriptid") ) {
					scripts.put(this.currentFile+":"+atts.getValue("id"), new ScriptEntry(Integer.parseInt(atts.getValue("db"))));
					
					if( !this.doNotUninstall ) {
						db.update("DELETE FROM scripts WHERE id="+Integer.parseInt(atts.getValue("db")));
					}
				}
				else if( tag.equals("usedby") ) {
					addParseFiles.add(atts.getValue("file"));	
				}
			}
		}

		@Override
		public void startPrefixMapping(String prefix, String uri) throws SAXException {
			// EMPTY
		}
	}
	
	/**
	 * Gibt Informationen zum geparsten Installationsscript zurueck.
	 * @param data Die Klasse der Informationen (questids, answerids, dialogids, scriptids)
	 * @return Eine Map, in der die QuestXML-ID Schluessel und die ID in der DB Wert sind
	 */
	// TODO: Enums statt einfachen Strings
	public Map<String,Integer> getInstallData(String data) {
		Map<String,Integer> result;	
		
		if( data.equals("questids") ) {
			result = this.questids;	
		}
		else if( data.equals("answerids") ) {
			result = this.answerids;	
		}
		else if( data.equals("dialogids") ) {
			result = this.dialogids;	
		}
		else if( data.equals("scriptids") ) {
			result = new HashMap<String,Integer>();
			
			for( String key : this.scripts.keySet() ) {
				result.put(key, this.scripts.get(key).id);	
			}
		}
		else {
			throw new RuntimeException("getInstallData: ungueltiger data-parameter >"+data+"<\n");
		}
		
		Map<String,Integer> newresult = new HashMap<String,Integer>();		
		for( Map.Entry<String, Integer> entry: result.entrySet()) {
			// Nur die Attribut-ID verwenden (der Teil hinter dem :). Den Dateinamen verwerfen.
			String[] tmp = StringUtils.splitPreserveAllTokens(entry.getKey(), ':');
			
			newresult.put(tmp[1], entry.getValue());		
		}

		return newresult;
	}
	
	protected String basename( String file ) {
		int pos = file.lastIndexOf('/');
		if( pos > -1 ) {
			file = file.substring(pos+1);
		}
		return file;
	}
	
	protected String basename( String file, String suffix ) {
		file = basename(file);
		if( file.endsWith(suffix) ) {
			file = file.substring(0, file.length() - suffix.length());
		}
		return file;
	}
	
	private void loadInstallData(String file) {
		this.questids.clear();
		this.answerids.clear();
		this.dialogids.clear();
		this.scripts.clear();
		
		File installXML = new File(config.get("QUESTPATH")+file+".install");
		// Wurde das Quest bereits installiert?
		if( installXML.exists() ) {
			try {
				XMLReader parser = XMLReaderFactory.createXMLReader();
				parser.setContentHandler(new InstallParser("", true));
			
				InputStream in = new FileInputStream(installXML);
				try
				{
					parser.parse(new InputSource(in));
				}
				finally
				{
					in.close();
				}
			}
			catch( Exception e ) {
				// TODO: Runtime-Exceptions sind suboptimal
				throw new RuntimeException("Konnte .install-XML '"+installXML+"' nicht parsen: "+e, e);
			}
		}
	}
	
	private class QuestParser implements ContentHandler {
		private Set<String> validTags = new HashSet<String>();
		private String currentFile;
		private Database db;
		private Stack<String> currentTag = new Stack<String>();
		
		private Stack<Map<String,Object>> currentData = new Stack<Map<String,Object>>();
		private int subScriptID = 0;
		
		private String currentInjectScript = "";
		
		/**
		 * Konstruktor.
		 * @param currentFile Der Name der mit diesen Parser uebersetzten Datei
		 */
		public QuestParser(String currentFile) {
			this.db = ContextMap.getContext().getDatabase();
			
			this.validTags.add("quest");
			this.validTags.add("answer");
			this.validTags.add("dialog");
			this.validTags.add("script");
			this.validTags.add("answerid");
			this.validTags.add("questid");
			this.validTags.add("dialogid");
			this.validTags.add("scriptid");
			this.validTags.add("inject");
			this.validTags.add("require");
			this.validTags.add("injectscript");
			this.validTags.add("part");
			
			this.currentFile = currentFile;
		}
		
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			String data = new String(ch, start, length);		
			Map<String,Object> currentdata = this.currentData.peek();
			if( !currentdata.containsKey("chardata") && !currentdata.containsKey("data") ) {
				// ltrim
				data = data.replaceAll("^\\s+", "");
			}
			String currentTag = this.currentTag.peek();
			if( currentTag.equals("dialog") || currentTag.equals("answer") ) {
				if( !currentdata.containsKey("chardata") ) {
					currentdata.put("chardata", data);
				}
				else {
					currentdata.put("chardata", currentdata.get("chardata") + data);
				}
			}
			else if( currentTag.equals("script") || currentTag.equals("part") ) {
				ScriptEntry tmpdata = (ScriptEntry)currentdata.get("data");
				
				tmpdata.script.set(this.subScriptID, tmpdata.script.get(this.subScriptID)+data);
			}
		}

		@Override
		public void endDocument() throws SAXException {
			// EMPTY
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			Map<String,Object> currentdata = this.currentData.pop();
			
			if( this.validTags.contains(localName.toLowerCase()) ) {
				if( this.currentTag.peek().equals("answer") ) {
					final String key = this.currentFile+":"+currentdata.get("id");
					
					if( !answerids.containsKey(key) ) {
						db.update("INSERT INTO quests_answers (text) VALUES ('"+Common.trimLines((String)currentdata.get("chardata"))+"');");
						answerids.put(key, db.insertID());
					}
					else {
						db.update("INSERT INTO quests_answers (id,text) VALUES ('"+answerids.get(key)+"','"+Common.trimLines((String)currentdata.get("chardata"))+"');");
					}
					addInstallData(this.currentFile, "<answerid id=\""+currentdata.get("id")+"\" db=\""+answerids.get(key)+"\" />\n");
				}	
				else if( this.currentTag.peek().equals("dialog") ) {
					final String key = this.currentFile+":"+currentdata.get("id");
					
					if( !dialogids.containsKey(key) ) {
						db.update("INSERT INTO quests_text (text,picture,comment) VALUES ('"+Common.trimLines((String)currentdata.get("chardata"))+"','"+currentdata.get("picture")+"','"+currentdata.get("comment")+"');");
						dialogids.put(key, db.insertID());
					}
					else {
						db.update("INSERT INTO quests_text (id,text,picture,comment) VALUES ('"+dialogids.get(key)+"','"+Common.trimLines((String)currentdata.get("chardata"))+"','"+currentdata.get("picture")+"','"+currentdata.get("comment")+"');");
					}
					addInstallData(this.currentFile, "<dialogid id=\""+currentdata.get("id")+"\" db=\""+dialogids.get(key)+"\" />\n");
				}	
				// <part...>...</part> bearbeiten (einfuegen von scriptteilen in scripte)
				else if( this.currentTag.peek().equals("part") ) {
					if( scripts.containsKey(this.currentInjectScript) ) {
						ScriptEntry data = (ScriptEntry)currentdata.get("data");
						
						Integer indexid = scripts.get(this.currentInjectScript).injectindex.get(currentdata.get("id"));
						if( indexid == null ) {
							throw new RuntimeException("QuestXML: illegaler/kein inject-index fuer "+currentdata.get("id")+"\n");	
						}
						String tmp = scripts.get(this.currentInjectScript).script.get(indexid.intValue()) + " "+data.script.get(0)+" ";
						scripts.get(this.currentInjectScript).script.set(indexid.intValue(), tmp);
					}
					else {
						throw new RuntimeException("QuestXML ("+this.currentFile+"): inject-target >"+this.currentInjectScript+"< nicht vorhanden\n");	
					}
				}
				// <script...>...</script> bearbeiten (Scripte)
				// - Der finale Eintrag erfolgt aber erst am Ende von parsexml(...), 
				//   nachdem sichergestellt ist, dass alle <part>-Tags verarbeitet wurden
				else if( this.currentTag.peek().equals("script") ) {
					final String key = this.currentFile+":"+currentdata.get("id");

					int id = 0;
					if( !scripts.containsKey(key) ) {
						db.update("INSERT INTO scripts (name,script) VALUES ('"+key+"','');");
						id = db.insertID();
					}
					else {	
						db.update("INSERT INTO scripts (id,name,script) VALUES ('"+scripts.get(key).id+"','"+key+"','');");
						id = scripts.get(key).id;
					}
					
					addInstallData(this.currentFile, "<scriptid id=\""+currentdata.get("id")+"\" db=\""+id+"\" />\n");
					
					scripts.put(key, (ScriptEntry)currentdata.get("data"));
					scripts.get(key).id = id;
				}	
			}
			
			this.currentTag.pop();
		}

		@Override
		public void endPrefixMapping(String prefix) throws SAXException {
			// EMPTY
		}

		@Override
		public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
			// EMPTY
		}

		@Override
		public void processingInstruction(String target, String data) throws SAXException {
			// EMPTY
		}

		@Override
		public void setDocumentLocator(Locator locator) {
			// EMPTY
		}

		@Override
		public void skippedEntity(String name) throws SAXException {
			// EMPTY
		}

		@Override
		public void startDocument() throws SAXException {
			// EMPTY
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
			if( this.validTags.contains(localName.toLowerCase()) ) {
				this.currentTag.push(localName.toLowerCase());

				this.currentData.push(new HashMap<String,Object>());
				Map<String,Object> currentdata = this.currentData.peek();
		
				if( this.currentTag.peek().equals("quest") ) {
					if( (atts.getValue("id") != null) && (atts.getValue("name") != null) ) {
						final String key = this.currentFile+":"+atts.getValue("id");
						
						if( !questids.containsKey(key) ) {
							db.update("INSERT INTO quests (name,qid) VALUES ('"+atts.getValue("name")+"','"+atts.getValue("id")+"');");
							questids.put(key, db.insertID());
						}
						else {
							db.update("INSERT INTO quests (id,name,qid) VALUES ('"+questids.get(key)+"','"+atts.getValue("name")+"','"+atts.getValue("id")+"');");
						}
						addInstallData(this.currentFile, "<questid id=\""+atts.getValue("id")+"\" db=\""+questids.get(key)+"\" />\n");
					}	
				}
				else if( this.currentTag.peek().equals("answer") ) {
					currentdata.put("id", atts.getValue("id"));	
				}
				else if( this.currentTag.peek().equals("dialog") ) {
					currentdata.put("id", atts.getValue("id"));
					currentdata.put("picture", atts.getValue("picture"));
					currentdata.put("comment", atts.getValue("comment"));
				}
				else if( this.currentTag.peek().equals("script") ) {
					ScriptEntry script = new ScriptEntry();
					script.script.add("");
					currentdata.put("data", script);
					currentdata.put("id", atts.getValue("id"));
					this.subScriptID = 0;
				}
				else if( this.currentTag.peek().equals("inject") ) {
					ScriptEntry tmpdata = (ScriptEntry)this.currentData.get(this.currentData.size()-2).get("data");
					tmpdata.injectindex.put(atts.getValue("id"), this.subScriptID + 1);
					tmpdata.script.add(this.subScriptID+1, "");
					tmpdata.script.add(this.subScriptID+2, "");
					this.subScriptID += 2;
				}
				else if( this.currentTag.peek().equals("questid") ) {
					ScriptEntry tmpdata = (ScriptEntry)this.currentData.get(this.currentData.size()-2).get("data");
					if( atts.getValue("id").indexOf(':') == -1 ) {
						String value = tmpdata.script.get(this.subScriptID) + questids.get(this.currentFile+":"+atts.getValue("id"));
						tmpdata.script.set(this.subScriptID, value);
					}
					else {
						String value = tmpdata.script.get(this.subScriptID) + questids.get(atts.getValue("id"));
						tmpdata.script.set(this.subScriptID, value);
					}
				}
				else if( this.currentTag.peek().equals("answerid") ) {
					ScriptEntry tmpdata = (ScriptEntry)this.currentData.get(this.currentData.size()-2).get("data");
					if( atts.getValue("id").indexOf(':') == -1 ) {
						String value = tmpdata.script.get(this.subScriptID) + answerids.get(this.currentFile+":"+atts.getValue("id"));
						tmpdata.script.set(this.subScriptID, value);
					}
					else {
						String value = tmpdata.script.get(this.subScriptID) + answerids.get(atts.getValue("id"));
						tmpdata.script.set(this.subScriptID, value);
					}
				}
				else if( this.currentTag.peek().equals("dialogid") ) {
					ScriptEntry tmpdata = (ScriptEntry)this.currentData.get(this.currentData.size()-2).get("data");
					if( atts.getValue("id").indexOf(':') == -1 ) {
						String value = tmpdata.script.get(this.subScriptID) + dialogids.get(this.currentFile+":"+atts.getValue("id"));
						tmpdata.script.set(this.subScriptID, value);
					}
					else {
						String value = tmpdata.script.get(this.subScriptID) + dialogids.get(atts.getValue("id"));
						tmpdata.script.set(this.subScriptID, value);
					}
				}
				else if( this.currentTag.peek().equals("scriptid") ) {
					ScriptEntry tmpdata = (ScriptEntry)this.currentData.get(this.currentData.size()-2).get("data");
					if( atts.getValue("id").indexOf(':') == -1 ) {
						String value = tmpdata.script.get(this.subScriptID) + scripts.get(this.currentFile+":"+atts.getValue("id")).id;
						tmpdata.script.set(this.subScriptID, value);
					}
					else {
						String value = tmpdata.script.get(this.subScriptID) + scripts.get(atts.getValue("id")).id;
						tmpdata.script.set(this.subScriptID, value);
					}
				}
				else if( this.currentTag.peek().equals("injectscript") ) {
					this.currentInjectScript = atts.getValue("id");
				}
				else if( this.currentTag.peek().equals("part") ) {
					ScriptEntry script = new ScriptEntry();
					script.script.add("");
					currentdata.put("data", script);
					currentdata.put("id", atts.getValue("id"));
					this.subScriptID = 0;
				}
				else if( this.currentTag.peek().equals("require") ) {
					addInstallData(this.currentFile, "<depend file=\""+atts.getValue("file")+"\" />\n");
					parseAddXml( atts.getValue("file") );
					addInstallData(basename(atts.getValue("file"),".xml"), "<usedby file=\""+this.currentFile+".xml\" />\n");
				}
			}
		}

		@Override
		public void startPrefixMapping(String prefix, String uri) throws SAXException {
			// EMPTY
		}
		
		/**
		 * Parst im aktuellen Parser-Objekt eine weitere Quest-XML.
		 * @param file Die zu parsende XML
		 */
		public void parseAddXml(String file) {
			String mycurrentfile = basename(file,".xml");
			
			if( compiledFiles.contains(mycurrentfile) ) {
				return;	
			}
			
			MESSAGE.get().append("parsing "+file+"...\n");
			
			String tmpcurrentfile = this.currentFile;
			Stack<String> tmpcurrenttag = this.currentTag;
			Stack<Map<String,Object>> tmpcurrentdata = this.currentData;
			
			this.currentFile = mycurrentfile;
			
			// Wurde das Quest bereits installiert?
			File installFile = new File(config.get("QUESTPATH")+this.currentFile+".install");
			if( installFile.exists() ) {
				try {
					XMLReader parser = XMLReaderFactory.createXMLReader();
					parser.setContentHandler(new InstallParser("", true));
				
					InputStream in = new FileInputStream(installFile);
					try
					{
						parser.parse(new InputSource(in));
					}
					finally {
						in.close();
					}
				}
				catch( Exception e ) {
					// TODO: Runtime-Exceptions sind suboptimal
					throw new RuntimeException("Konnte .install-XML '"+installFile+"' nicht parsen: "+e, e);
				}
			}
			
			addInstallData(this.currentFile, "<?xml version='1.0' encoding='UTF-8'?>\n");
			addInstallData(this.currentFile, "<install>\n");
			
			File questFile = new File(config.get("QUESTPATH")+file);
			
			try {
				XMLReader parser = XMLReaderFactory.createXMLReader();
				parser.setContentHandler(this);
			
				InputStream in = new FileInputStream(questFile);
				try
				{
					parser.parse(new InputSource(in));
				}
				finally
				{
					in.close();
				}
			}
			catch( Exception e ) {
				// TODO: Runtime-Exceptions sind suboptimal
				throw new RuntimeException("Konnte QuestXML '"+this.currentFile+"' nicht parsen: "+e, e);
			}
			
			compiledFiles.add(this.currentFile);
		
			this.currentFile = tmpcurrentfile;
			this.currentTag = tmpcurrenttag;
			this.currentData = tmpcurrentdata;
		}
	}
	
	protected void addInstallData(String file, String data) {
		if( !installFiles.containsKey(file) ) {
			installFiles.put(file, data);
		}
		else {
			installFiles.put(file, installFiles.get(file)+data);
		}
	}
	
	protected void addInstallData(String data) {
		addInstallData(this.currentFile, data);
	}
	
	private void parsexml(String file) {
		MESSAGE.get().append("parsing "+file+"...\n");
		
		File questFile = new File(config.get("QUESTPATH")+file);
		if( !questFile.exists() ) {
			MESSAGE.get().append("QuestXML konnte nicht gefunden werden");
			return;
		}
			
		this.currentFile = basename(file,".xml");
		
		this.compiledFiles.add(this.currentFile);
		
		// Wurde das Quest bereits installiert?
		File installFile = new File(config.get("QUESTPATH")+this.currentFile+".install");
		if( installFile.exists() ) {
			try {
				XMLReader parser = XMLReaderFactory.createXMLReader();
				parser.setContentHandler(new InstallParser("", false));
			
				parser.parse(new InputSource(new FileInputStream(installFile)));			
			}
			catch( Exception e ) {
				// TODO: Runtime-Exceptions sind suboptimal
				throw new RuntimeException("Konnte .install-XML nicht parsen: "+e, e);
			}
		}
		
		addInstallData("<?xml version='1.0' encoding='UTF-8'?>\n");
		addInstallData("<install>\n");
		
		QuestParser questParser = new QuestParser(this.currentFile);
		
		try {
			XMLReader parser = XMLReaderFactory.createXMLReader();
			parser.setContentHandler(questParser);
		
			parser.parse(new InputSource(new FileInputStream(questFile)));			
		}
		catch( Exception e ) {
			// TODO: Runtime-Exceptions sind suboptimal
			throw new RuntimeException("Konnte QuestXML '"+this.currentFile+"' nicht parsen: "+e, e);
		}
		
		// Abhaengigkeiten verarbeiten (<usedby>-tag)
		for( int i=0; i < this.addParseFiles.size(); i++ ) {
			questParser.parseAddXml( file );	
		}
		this.addParseFiles.clear();
		
		Database db = ContextMap.getContext().getDatabase();
		
		// process scripts
		for( ScriptEntry script : this.scripts.values() ) {
			db.update("UPDATE scripts SET script='"+Common.trimLines(Common.implode(" ",script.script))+"' WHERE id='"+script.id+"';");	
		}
		
		// process install-files
		for( String filename : this.installFiles.keySet() ) {
			String instfile = this.installFiles.get(filename);
			instfile += "</install>\n";
			installFile = new File(config.get("QUESTPATH")+filename+".install");
			if( installFile.exists() ) {
				installFile.delete();
			}
			
			try {
				FileWriter writer = new FileWriter(installFile);
				writer.write(instfile);
				writer.close();
			
				if( SystemUtils.IS_OS_UNIX ) {
					Runtime.getRuntime().exec("chmod 0666 "+installFile.getAbsolutePath());
				}
			}
			catch( IOException e ) {
				MESSAGE.get().append("Konnte .install-Datei fuer QuestXML '"+instfile+"' nicht schreiben: "+e);
			}
		}
	}
}
