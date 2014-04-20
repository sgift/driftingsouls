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
package net.driftingsouls.ds2.server.modules.ks;

import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Zeigt das Kampflog an.
 * @author Christopher Jung
 *
 */
public class KSMenuHistoryAction extends BasicKSMenuAction implements ContentHandler {
	private static final Log log = LogFactory.getLog(KSMenuHistoryAction.class);
	private String text = "";
	private boolean showOK = true;
	private boolean showTakeCommand = false;
	
	private StringBuilder history_text = new StringBuilder();
	private String history_tag = "";
	private final Set<String >history_validTags = new HashSet<>();
	private boolean trimHistory = false;
	private int historyPage = -1;
	private int historyCurrentpage = 0;
	private int historyMaxpage = 0;
	private Map<Integer,String> historySides = new HashMap<>();
	private boolean historyShowtag = true;
	
	private final Map<Integer,Boolean> filter = new HashMap<>();

	/**
	 * Konstruktor.
	 *
	 */
	public KSMenuHistoryAction() {
		this.history_validTags.add("battle");
		this.history_validTags.add("fileinfo");
		this.history_validTags.add("coords");
		this.history_validTags.add("side1");
		this.history_validTags.add("side2");
		this.history_validTags.add("startdate");
		this.history_validTags.add("action");
		this.history_validTags.add("endturn");
		
		this.historySides.put( -1, "Das Tickscript" );
		
		this.text = "";
		this.showOK = true;
		this.showTakeCommand = false;
	}
	
	/**
	 * Setzt den ueber dem Kampflog anzuzeigenden Text .
	 * @param text Der Text
	 */
	public void setText(String text) {
		this.text = text;
	}
	
	/**
	 * Gibt an, ob der OK-Buttom zum schliessen des Kampflogs angezeigt werden soll.
	 * @param value <code>true</code>, falls der OK-Button angezeigt werden soll
	 */
	public void showOK(boolean value) {
		this.showOK = value;
	}
	
	/**
	 * Gibt an, ob die Schlachtflaeche zur Uebernahme des Kampfes angezeigt werden soll.
	 * @param value <code>true</code>, falls die Schaltflaeche angezeigt werden soll
	 */
	public void showTakeCommand(boolean value) {
		this.showTakeCommand = value;
	}
	
	/**
	 * Prueft, ob die aktuelle Seite ({@link #historyCurrentpage}) angezeigt werden soll
	 * oder nicht.
	 * 
	 * @return <code>true</code>, falls die aktuelle Seite angezeigt werden soll
	 */
	private boolean showCurrentPage() {
		// Wenn eine Seite des Logs ausgewaehlt ist, dann pruefen, ob die aktuelle Seite diese Seite ist
		if( this.historyPage != -1 ) {
			return this.historyPage == this.historyCurrentpage;
		}
		
		// Wenn keine Seite des Logs ausgewaehlt, dann die letzte Seite anzeigen
		return this.historyCurrentpage == this.historyMaxpage;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
	{
		Context context = ContextMap.getContext();
		this.historyShowtag = true;

		if( this.history_validTags.contains(localName.toLowerCase()) )
		{
			this.history_tag = localName.toLowerCase();
			this.trimHistory = true;
			
			int side = 0;
			if( atts.getValue("side") != null )
			{
				side = Integer.parseInt(atts.getValue("side"));
			}
	
			if( this.history_tag.equals("endturn") ) {
				if( atts.getValue("type").equals("all") )
				{
					if( showCurrentPage() )
					{
						if( (side == -1) || this.filter.get(side) )
						{
							this.history_text.append("[tooltip="+Common.date("d.m.Y H:i:s", Long.parseLong(atts.getValue("time")))+"][img]./data/interface/ks/icon_side"+side+".png[/img][/tooltip] ");
							this.history_text.append(this.historySides.get(side)).append(" hat die Runde beendet\n");
						}
						else
						{
							this.historyShowtag = false;
						}
					}
					this.historyCurrentpage++;
					this.historyMaxpage++;
					if( this.historyPage == -1 )
					{
						this.history_text.setLength(0);
					}
				}
				else
				{
					if( showCurrentPage() )
					{
						if( (side > -1) && !this.filter.get(side) )
						{
							this.historyShowtag = false;
							return;	
						}
						this.history_text.append("[tooltip="+Common.date("d.m.Y H:i:s", Long.parseLong(atts.getValue("time")))+"][img]./data/interface/ks/icon_side"+side+".png[/img][/tooltip] ");
						this.history_text.append(this.historySides.get(side)).append(" hat die Runde beendet\n");
					}
				}
			} 
			else if( this.history_tag.equals("action") && showCurrentPage() )
			{
				
				if( (side > -1) && !this.filter.get(side) )
				{
					this.historyShowtag = false;
					return;	
				}
						
				this.history_text.append("[tooltip="+Common.date("d.m.Y H:i:s", Long.parseLong(atts.getValue("time")))+"][img]./data/interface/ks/icon_side"+side+".png[/img][/tooltip] ");
			} 
			else if( this.history_tag.equals("side1") || this.history_tag.equals("side2") )
			{
				int thisSide = 0;
				if( this.history_tag.equals("side2") )
				{
					thisSide = 1;
				}
				User auser = (User)context.getDB().get(User.class, Integer.parseInt(atts.getValue("commander")));
				if( auser == null )
				{
					this.historySides.put(thisSide, "Unbekannter Spieler ("+atts.getValue("commander")+")");
				}
				else
				{
					this.historySides.put(thisSide, "<a class=\"profile\" style=\"color:#000050\" href=\""+Common.buildUrl("default", "module", "userprofile", "user", auser.getId())+"\">"+Common._titleNoFormat(auser.getName())+"</a>");
				}
			} 
		}
		else
		{
			List<String> params = new ArrayList<>();
	
			for( int i=0; i < atts.getLength(); i++ )
			{
				params.add(atts.getQName(i)+"=\""+atts.getValue(i)+"\"");
			}

			if( showCurrentPage()  )
			{
				this.history_text.append("<").append(qName).append(" ").append(Common.implode(" ", params)).append(">");
			}
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if( !this.historyShowtag ) {
			return;
		}
		if( !this.history_tag.equals("action") ) {
			return;
		}
	
		String data = new String(ch, start, length);
		if( this.trimHistory ) {
			this.trimHistory = false;
			data = data.trim();
		}
	
		if( showCurrentPage() ) {
			this.history_text.append(data);
		}		
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if( !this.historyShowtag ) {
			return;
		}
		if( !this.history_validTags.contains(localName.toLowerCase()) ) {
			this.history_text.append("</").append(localName).append(">");
		}		
	}
	
	@Override
	public void endDocument() throws SAXException {
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
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		// EMPTY
	}
	
	@Override
	public Result execute(Battle battle) throws IOException {
		Result result = super.execute(battle);
		if( result != Result.OK ) {
			return result;
		}
		
		TemplateEngine t = this.getController().getTemplateEngine();
		Context context = ContextMap.getContext();
		
		/*
		 *  Filter verarbeiten
		 */
		String modlogpagefilter = context.getRequest().getParameterString("modlogpagefilter");
		String logpagefilter = context.getRequest().getParameterString("logpagefilter");
		
		filter.put(0, true);
		filter.put(1, true);
				 
		if( modlogpagefilter.length() > 0 ) {
			logpagefilter = modlogpagefilter;	
		}
		
		if( logpagefilter.length() > 2 ) {
			int side = Integer.parseInt(""+logpagefilter.charAt(1));
			char mode = logpagefilter.charAt(2);

			filter.put(side, mode != 'h');	
		}
		
		if( (context.getRequest().getParameter("logpage") != null) || !filter.get(0) || !filter.get(1) ) {
			String filterstr = "";
			if( !filter.get(0) ) {
				filterstr = "s0h";
			}
			else if( !filter.get(1) ) {
				filterstr = "s1h";
			}
			t.setVar(	"global.logpagestr",		"&amp;logpage="+context.getRequest().getParameterInt("logpage")+"&amp;logpagefilter="+filterstr,
						"global.showlog.filter",	filterstr );
		}
		
		t.setVar(	"global.showlog.side0.show",	filter.get(0),
					"global.showlog.side1.show",	filter.get(1));

		
		this.historyPage = -1;
		if( context.getRequest().getParameter("logpage") != null ) {
			this.historyPage = context.getRequest().getParameterInt("logpage");
		}

		t.setBlock("_ANGRIFF","global.showlog.turnlist.item","global.showlog.turnlist.list");
	
		String actionstr = "";
		if( this.showOK ) {
			actionstr = "&amp;ksaction=history";
		}
		
		t.setVar(	"global.showlog",				1,
					"global.showlog.text",			this.text,
					"global.showlog.okbutton",		this.showOK,
					"global.showlog.takecommand",	this.showTakeCommand,
					"global.showlog.actionstr",		actionstr );
		
		try {
			File ksLog = new File(Configuration.getLogPath()+"battles/battle_id"+battle.getId()+".log");
			if( !ksLog.isFile() ) {
				t.setVar( "global.showlog.log", "Fehler: Konnte Kampflog nicht &ouml;ffnen");
				return Result.ERROR;
			}
			
			parseLog(ksLog);
			
			BBCodeParser bbcodeparser = BBCodeParser.getNewInstance();
			bbcodeparser.registerHandler( "tooltip", 2, "<a class='tooltip' href=\"#\">$1<span class='ttcontent'>$2</span></a>" );
		
			for( int i=0; i <= this.historyMaxpage; i++ ) {
				t.setVar(	"global.showlog.turnlist.pageid",	i,
							"global.showlog.turnlist.page",		i+1 );
				t.parse("global.showlog.turnlist.list", "global.showlog.turnlist.item", true);
			}
		
			t.setVar("global.showlog.log", StringUtils.replace(bbcodeparser.parse(this.history_text.toString()), "\n", "<br />"));
		}
		catch( SAXException | IOException e ) {
			t.setVar("global.showlog.log", "Fehler beim Anzeigen der Kampfhistorie: "+e);
			log.error("", e);
		}

		return Result.OK;
	}

	private void parseLog(File ksLog) throws SAXException, IOException
	{
		XMLReader parser = XMLReaderFactory.createXMLReader();
		
		parser.setContentHandler(this);
		try (InputStream in = new SequenceInputStream(new FileInputStream(ksLog), new ByteArrayInputStream("</battle>".getBytes())))
		{
			parser.parse(new InputSource(in));
		}
	}
}
