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

import net.driftingsouls.ds2.server.battles.*;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.services.BattleService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Zeigt das Kampflog an.
 * @author Christopher Jung
 *
 */
@Component
public class KSMenuHistoryAction extends BasicKSMenuAction {
	private String text;
	private boolean showOK;
	private boolean showTakeCommand;

	private int historyPage = -1;
	private int historyCurrentpage = 0;
	private int historyMaxpage = 0;

	private final StringBuilder historyText = new StringBuilder();
	private final Map<Integer,String> historySides = new HashMap<>();
	private final Map<Integer,Boolean> filter = new HashMap<>();
	private final BBCodeParser bbCodeParser;

	/**
	 * Konstruktor.
	 *
	 */
	public KSMenuHistoryAction(BattleService battleService, BBCodeParser bbCodeParser) {
		super(battleService, null);
		this.bbCodeParser = bbCodeParser;
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
	public Result execute(TemplateEngine t, Battle battle) throws IOException {
		Result result = super.execute(t, battle);
		if( result != Result.OK ) {
			return result;
		}
		
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

		if( battle.getSchlachtLog() != null )
		{
			parseLog(battle.getSchlachtLog());
		}

		bbCodeParser.registerHandler( "tooltip", 2, "<a class='tooltip' href=\"#\">$1<span class='ttcontent'>$2</span></a>" );

		for( int i=0; i <= this.historyMaxpage; i++ ) {
			t.setVar(	"global.showlog.turnlist.pageid",	i,
						"global.showlog.turnlist.page",		i+1 );
			t.parse("global.showlog.turnlist.list", "global.showlog.turnlist.item", true);
		}

		t.setVar("global.showlog.log", bbCodeParser.parse(this.historyText.toString()).replace("\n", "<br />"));

		return Result.OK;
	}

	private void parseLog(SchlachtLog ksLog) {
		for (SchlachtLogEintrag eintrag : ksLog.getEintraege())
		{
			if( eintrag instanceof SchlachtLogAktion )
			{
				erzeugeAnzeige((SchlachtLogAktion)eintrag);
			}
			else if( eintrag instanceof SchlachtLogKommandantWechselt )
			{
				erzeugeAnzeige((SchlachtLogKommandantWechselt)eintrag);
			}
			else if( eintrag instanceof SchlachtLogRundeBeendet )
			{
				erzeugeAnzeige((SchlachtLogRundeBeendet) eintrag);
			}
		}
	}

	private void erzeugeAnzeige(SchlachtLogAktion eintrag)
	{
		if ((eintrag.getSeite() > -1) && !this.filter.get(eintrag.getSeite()))
		{
			return;
		}

		if( showCurrentPage() ) {
			this.historyText.append("[tooltip=").append(Common.date("d.m.Y H:i:s", eintrag.getZeitpunkt())).append("][img]./data/interface/ks/icon_side").append(eintrag.getSeite()).append(".png[/img][/tooltip] ");
			this.historyText.append(eintrag.getText().trim());
			this.historyText.append("\n\n");
		}
	}

	private void erzeugeAnzeige(SchlachtLogKommandantWechselt eintrag)
	{
		int thisSide = eintrag.getSeite();
		Context context = ContextMap.getContext();
		User auser = (User)context.getDB().get(User.class, eintrag.getUserId());
		if( auser == null )
		{
			this.historySides.put(thisSide, Common._titleNoFormat(bbCodeParser, eintrag.getName())+" ("+eintrag.getUserId()+")");
		}
		else
		{
			this.historySides.put(thisSide, "<a class=\"profile\" style=\"color:#000050\" href=\""+Common.buildUrl("default", "module", "userprofile", "user", auser.getId())+"\">"+Common._titleNoFormat(bbCodeParser, auser.getName())+"</a>");
		}
	}

	private void erzeugeAnzeige(SchlachtLogRundeBeendet rbEintrag)
	{
		if( rbEintrag.getTyp() == SchlachtLogRundeBeendet.Modus.ALLE )
		{
			if( showCurrentPage() )
			{
				if( (rbEintrag.getSeite() == -1) || this.filter.get(rbEintrag.getSeite()) )
				{
					this.historyText.append("[tooltip=").append(Common.date("d.m.Y H:i:s", rbEintrag.getZeitpunkt())).append("]");
					this.historyText.append("[img]./data/interface/ks/icon_side").append(rbEintrag.getSeite()).append(".png[/img][/tooltip] ");
					this.historyText.append(this.historySides.get(rbEintrag.getSeite())).append(" hat die Runde beendet\n");
					this.historyText.append("\n\n");
				}
			}
			this.historyCurrentpage++;
			this.historyMaxpage++;
			if( this.historyPage == -1 )
			{
				this.historyText.setLength(0);
			}
		}
		else
		{
			if( showCurrentPage() )
			{
				if( (rbEintrag.getSeite() > -1) && !this.filter.get(rbEintrag.getSeite()) )
				{
					return;
				}
				this.historyText.append("[tooltip=").append(Common.date("d.m.Y H:i:s", rbEintrag.getZeitpunkt())).append("][img]./data/interface/ks/icon_side").append(rbEintrag.getSeite()).append(".png[/img][/tooltip] ");
				this.historyText.append(this.historySides.get(rbEintrag.getSeite())).append(" hat die Runde beendet\n");
				this.historyText.append("\n\n");
			}
		}
	}
}
