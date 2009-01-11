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
package net.driftingsouls.ds2.server.modules.admin;

import java.io.IOException;
import java.io.Writer;
import java.sql.Blob;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.AdminController;
import net.driftingsouls.ds2.server.scripting.ScriptParserContext;
import net.driftingsouls.ds2.server.scripting.entities.CompletedQuest;
import net.driftingsouls.ds2.server.scripting.entities.Quest;
import net.driftingsouls.ds2.server.scripting.entities.QuickQuest;
import net.driftingsouls.ds2.server.scripting.entities.RunningQuest;

import org.apache.commons.lang.StringUtils;

/**
 * Ermoeglicht das Verwalten von Quick-Quests.
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Quests", name="QuickQuests")
public class QuestsQuick implements AdminPlugin {
	@Override
	public void output(AdminController controller, String page, int action) throws IOException {
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		
		int id = context.getRequest().getParameterInt("id");
		String qact = context.getRequest().getParameterString("qact");
		
		org.hibernate.Session db = context.getDB();
		
		// Aktiviert das Quest
		if( qact.equals("enable") ) {
			QuickQuest qquest = (QuickQuest)db.get(QuickQuest.class, id);
			
			Quest questdata = (Quest)db.createQuery("from Quest where qid= :qid")
				.setString("qid", qquest.getQid())
				.uniqueResult();
			
			if( questdata == null ) {
				questdata = new Quest(qquest.getQName());
				questdata.setQid(qquest.getQid());
				
				db.save(questdata);
			}
			qquest.setEnabled(questdata.getId());
			
			echo.append("Quest aktiviert<br /><br />");
		}
		// Deaktiviert das Quest
		else if( qact.equals("disable") ) {
			QuickQuest qquest = (QuickQuest)db.get(QuickQuest.class, id);
			
			ScriptEngine scriptparser = context.get(ContextCommon.class).getScriptParser("DSQuestScript");
			
			List<?> rquestList = db.createQuery("from RunningQuest where quest= :qid")
				.setInteger("qid", qquest.getEnabled())
				.list();
			for( Iterator<?> iter=rquestList.iterator(); iter.hasNext(); ) {
				RunningQuest rquest = (RunningQuest)iter.next();
				
				try {
					Blob execdata = rquest.getExecData();
					scriptparser.setContext(
							ScriptParserContext.fromStream(execdata.getBinaryStream())
					);
				}
				catch( Exception e ) {
					echo.append("WARNUNG: Konnte Questdaten nicht laden: Laufendes Quest "+rquest.getId()+"<br />\n");
				}
				
				final Bindings engineBindings = scriptparser.getContext().getBindings(ScriptContext.ENGINE_SCOPE);
				
				engineBindings.put("USER", rquest.getUser().getId());
				engineBindings.put("QUEST", "r"+rquest.getId());
				engineBindings.put("_PARAMETERS", "0");
				try {
					scriptparser.eval(":0\n!ENDQUEST\n!QUIT");
				}
				catch( ScriptException e ) {
					throw new RuntimeException(e);
				}
				echo.append("Beende Quest bei Spieler "+rquest.getUser().getId()+"<br />\n");
			}
			
			qquest.setEnabled(0);
			
			echo.append("Quest deaktiviert<br /><br />");
		}
		
		// Zeige Details zum Quest an
		if( qact.equals("details") ) {
			QuickQuest qquest = (QuickQuest)db.get(QuickQuest.class, id);
			
			echo.append(Common.tableBegin(550,"left"));
			if( qquest.getEnabled() != 0 ) {
				echo.append("Aktiv bei:<br /><ul>");
				List<?> rquestList = db.createQuery("from RunningQuest rq inner join fetch rq.user where rq.quest= :qid order by rq.user")
					.setInteger("qid", qquest.getEnabled())
					.list();
				for( Iterator<?> iter=rquestList.iterator(); iter.hasNext(); ) {
					RunningQuest rquest = (RunningQuest)iter.next();
					
					echo.append("<li>"+Common._title(rquest.getUser().getName())+" ("+rquest.getUser().getId()+")</li>\n");
				}
				echo.append("</ul><br />\n");
			}
			
			echo.append("Abgeschlossen von:<br /><ul>");
			List<?> cquestList = db.createQuery("from CompletedQuest cq inner join fetch cq.quest q inner join fetch cq.user " +
					"where q.qid = :qid order by cq.user")
				.setString("qid", qquest.getQid())
				.list();
			for( Iterator<?> iter=cquestList.iterator(); iter.hasNext(); ) {
				CompletedQuest cq = (CompletedQuest)iter.next();

				echo.append("<li>"+Common._title(cq.getUser().getName())+" ("+cq.getUser().getId()+")</li>\n");
			}
			echo.append("</ul><br />\n");
			
			echo.append(Common.tableEnd());
		}
		// Zeigt die Liste aller Quests an
		else if( !qact.equals("script") ) {
			echo.append(Common.tableBegin(550,"center"));
			echo.append("<table class=\"noBorderX\">\n");
			
			List<?> qquestList = db.createQuery("from QuickQuest order by qid").list();
			for( Iterator<?> iter=qquestList.iterator(); iter.hasNext(); ) {
				QuickQuest qquest = (QuickQuest)iter.next();
				
				echo.append("<tr><td class=\"noBorderX\">"+qquest.getQid()+"</td>\n");
				echo.append("<td class=\"noBorderX\">&nbsp;&nbsp;&nbsp;" +
						"<a class=\"forschinfo\" " +
							"href=\"./ds?module=admin&page="+page+"&act="+action+"&id="+qquest.getId()+"&qact=details\"" +
						">"+qquest.getQName()+"</a>" +
						"&nbsp;&nbsp;&nbsp;</td>\n");
				echo.append("<td class=\"noBorderX\">");
				if( qquest.getEnabled() == 0 ) {
					echo.append("[<a class=\"error\" href=\"./ds?module=admin&page="+page+"&act="+action+"&id="+qquest.getId()+"&qact=enable\">inaktiv</a>]");	
				}
				else {
					echo.append("[<a class=\"ok\" href=\"./ds?module=admin&page="+page+"&act="+action+"&id="+qquest.getId()+"&qact=disable\">aktiv</a>]");
				}
				/*echo.append(" - [<a class=\"forschinfo\" " +
				"href=\"./ds?module=admin&sess="+context.getSession()+"&page="+page+"&act="+action+"&id="+qquest.getId()+"&qact=script\"" +
				">export</a>]");*/
				echo.append("</td></tr>\n");
			}
			echo.append("</table>\n");
			echo.append(Common.tableEnd());
		}
		// Exportiert das Quest
		else if( qact.equals("script") ) {
			QuickQuest qquest = (QuickQuest)db.get(QuickQuest.class, id);
			
			if( (qquest.getSource().indexOf(',') > -1) || (qquest.getTarget().indexOf(',') > -1) ) {
				echo.append("ERROR: QuickQuest-Scripte unterst&uuml;tzen im Moment nur EINE source und EIN target<br />");
				return;
			}
			
			int sourceobjectid = Integer.parseInt(qquest.getSource());
			int targetobjectid = Integer.parseInt(qquest.getTarget());
			
			String qquest_desc = StringUtils.replace(qquest.getDescription(), "&", "&amp;");
			qquest_desc = StringUtils.replace(qquest_desc, ">", "&gt;");
			qquest_desc = StringUtils.replace(qquest_desc, "<", "&lt;");
			
			String qquest_shortdesc = StringUtils.replace(qquest.getShortDesc(), "&", "&amp;");
			qquest_shortdesc = StringUtils.replace(qquest_shortdesc, ">", "&gt;");
			qquest_shortdesc = StringUtils.replace(qquest_shortdesc, "<", "&lt;");
			
			String qquest_finishtext = StringUtils.replace(qquest.getFinishText(), "&", "&amp;");
			qquest_finishtext = StringUtils.replace(qquest_finishtext, ">", "&gt;");
			qquest_finishtext = StringUtils.replace(qquest_finishtext, "<", "&lt;");
			
			String qquest_notyettext = StringUtils.replace(qquest.getNotYetText(), "&", "&amp;");
			qquest_notyettext = StringUtils.replace(qquest_notyettext, ">", "&gt;");
			qquest_notyettext = StringUtils.replace(qquest_notyettext, "<", "&lt;");
				
			echo.append(Common.tableBegin(600,"left"));
			echo.append("<span class=\"nobr\">\n");
			
			Set<String> reqFileList = new HashSet<String>();
			if( qquest.getSourceType().equals("gtuposten") || 
				qquest.getTargetType().equals("gtuposten") ) {
				
				if( qquest.getSourceType().equals("gtuposten") ) {
					reqFileList.add("gtu-posten-"+sourceobjectid+".xml");
				}
				if( qquest.getTargetType().equals("gtuposten") ) {
					reqFileList.add("gtu-posten-"+targetobjectid+".xml");
				}
				reqFileList.add("gtu-posten-generic.xml");
			}
			
			if( qquest.getDependsOnQuests().length() > 0 ) {
				String[] dquests = StringUtils.split(qquest.getDependsOnQuests(), ';');
				for( int i=0; i < dquests.length; i++ ) {
					String[] tmp = StringUtils.split(dquests[i], ':');
					reqFileList.add(tmp[0]+".xml");	
				}	
			}
			
			echo.append("&lt;?xml version='1.0' encoding='UTF-8'?&gt;<br />\n");
			echo.append("&lt;quest id=\""+qquest.getQid()+"\" name=\""+qquest.getQName()+"\"&gt;<br />\n");
			for( String file : reqFileList ) {
				echo.append("&lt;require file=\""+file+"\" /&gt;<br />\n");	
			}
			
			/*
			 * Dialog Questbeschreibung ("info")
			 */
			echo.append("&lt;dialog id=\"info\" picture=\""+qquest.getHead()+"\"&gt;<br />\n");
			echo.append(nl2br(qquest_shortdesc)+"<br />\n");
			echo.append("[hr]<br />\n");
			echo.append(nl2br(qquest_desc)+"<br />\n<br />\n");
			if( !qquest.getReqItems().isEmpty() || (qquest.getReqRe() > 0) ) {
				echo.append("Benötigt:[color=red]<br />\n");
				if( !qquest.getReqItems().isEmpty() ) {
					ResourceList reslist = qquest.getReqItems().getResourceList();
					for( ResourceEntry res : reslist ) {
						echo.append("[resource="+res.getId()+"]"+res.getCount1()+"[/resource]<br />\n");	
					}
				}
				if( qquest.getReqRe() > 0 ) {
					echo.append(Common.ln(qquest.getReqRe())+" RE\n");
				}
				echo.append("[/color]<br /><br />\n\n");
			}
			if( !qquest.getAwardItems().isEmpty() ) {
				echo.append("Belohnung in Waren:<br />\n");
				
				ResourceList reslist = qquest.getAwardItems().getResourceList();
				for( ResourceEntry res : reslist ) {
					echo.append("[resource="+res.getId()+"]"+res.getCount1()+"[/resource]<br />\n");	
				}
			}
			if( qquest.getAwardRe() != 0 ) {
				echo.append("Belohnung in RE: "+Common.ln(qquest.getAwardRe())+"<br />\n");	
			}
			echo.append("&lt;/dialog&gt;<br />\n");
			
			/*
			 * Dialog Quest noch nicht erfuellt ("notyet")
			 */
			echo.append("&lt;dialog id=\"notyet\" picture=\""+qquest.getHead()+"\"&gt;<br />\n");
			if( qquest_notyettext.length() == 0 ) {
				echo.append("Tut mir leid. Du hast die Aufgabe noch nicht komplett erledigt.<br />\n");
			}
			else {
				echo.append(nl2br(qquest_notyettext)+"<br />\n");	
			}
			echo.append("&lt;/dialog&gt;<br />\n");
			
			/*
			 * Dialog Quest beendet ("ready")
			 */
			echo.append("&lt;dialog id=\"ready\" picture=\""+qquest.getHead()+"\"&gt;<br />\n");
			if( qquest_finishtext.length() == 0 ) {
				echo.append("Sehr gut! Du hast deine Aufgabe beendet.<br />\n");
				echo.append("Hier hast du ein paar Dinge die du sicher gut gebrauchen kannst:<br />\n<br />\n");
			}
			else {
				echo.append(nl2br(qquest_finishtext)+"<br />\n<br />\n");	
			}
			if( !qquest.getAwardItems().isEmpty() ) {
				echo.append("Belohnung in Waren:<br />\n");
				
				ResourceList reslist = qquest.getAwardItems().getResourceList();
				for( ResourceEntry res : reslist ) {
					echo.append("[resource="+res.getId()+"]"+res.getCount1()+"[/resource]<br />\n");	
				}
				echo.append("<br />\n");
			}
			if( qquest.getAwardRe() != 0 ) {
				echo.append("Belohnung in RE: "+Common.ln(qquest.getAwardRe())+"<br />\n");	
			}
			echo.append("&lt;/dialog&gt;<br />\n");
			
			/*
			 * Antworten 
			 */
			echo.append("&lt;answer id=\"yes\"&gt;Annehmen&lt;/answer&gt;<br />\n");
			echo.append("&lt;answer id=\"no\"&gt;Ablehnen&lt;/answer&gt;<br />\n");
			echo.append("&lt;answer id=\"endquest\"&gt;Auftrag &amp;gt;"+qquest.getQName()+"&amp;lt; beenden&lt;/answer&gt;<br />\n");
			echo.append("&lt;answer id=\"startquest\"&gt;Auftrag &amp;gt;"+qquest.getQName()+"&amp;lt;&lt;/answer&gt;<br />\n");
			
			/*
			 *	
			 *	Zuerst generieren wir den Code fuer das Zielobjekt
			 *	
			 */
			
			String tsParams = qquest.getQid()+"_finish";
			
			// Menu
			StringBuilder tsMenu = new StringBuilder();
			tsMenu.append("!LoadQuestContext &lt;questid id=\""+qquest.getQid()+"\" /&gt;<br />\n");
			tsMenu.append("!GETQUESTID #QUEST<br />\n");
			tsMenu.append("!COMPARE #A &lt;questid id=\""+qquest.getQid()+"\" /&gt;<br />\n");
			tsMenu.append("!JNE 0_questfinal"+qquest.getQid()+"_endcont<br />\n");
			
			tsMenu.append("!COMPARE #QSTATUS 1<br />\n");
			tsMenu.append("!JNE 0_questfinal"+qquest.getQid()+"_endcont<br />\n");
			
			tsMenu.append("!ADDANSWER &lt;answerid id=\"endquest\" /&gt; "+qquest.getQid()+"_finish<br />\n");
			tsMenu.append("!JUMP 0_questfinal"+qquest.getQid()+"_endcont<br />\n");
		
			tsMenu.append(":0_questfinal"+qquest.getQid()+"_endcont<br />\n<br />\n");
			
			// Code
			StringBuilder tsCode = new StringBuilder();
			tsCode.append(":"+qquest.getQid()+"_finish<br />\n");
			tsCode.append("!LoadQuestContext &lt;questid id=\""+qquest.getQid()+"\" /&gt;<br />\n");
			tsCode.append("!GETQUESTID #QUEST<br />\n");
			tsCode.append("!COMPARE #A &lt;questid id=\""+qquest.getQid()+"\" /&gt;<br />\n");
			tsCode.append("!JNE 0<br />\n");
			tsCode.append("!COMPARE #QSTATUS 1<br />\n");
			tsCode.append("!JNE 0<br />\n");
			
			// Die zum beenden benoetigten Items checken
			if( !qquest.getReqItems().isEmpty() ) {
				tsCode.append("// Resourcen ueberpruefen<br />\n");
				tsCode.append("!COPYVAR #ship shipsource.cargo<br />\n");
				ResourceList reslist = qquest.getReqItems().getResourceList();
				for( ResourceEntry res : reslist ) {
					if( res.getId().isItem() && (res.getId().getQuest() != 0) ) {
						tsCode.append("!HASQUESTITEM #ship "+res.getId().getItemID()+" "+res.getCount1()+"<br />\n");
						tsCode.append("!JLE "+qquest.getQid()+"_finish_notyet<br />\n");
						tsCode.append("!ADDQUESTITEM #ship "+res.getId().getItemID()+" -"+res.getCount1()+"<br />\n");
					}
					else {
						tsCode.append("!HASRESOURCE #ship "+res.getId()+" "+res.getCount1()+"<br />\n");
						tsCode.append("!JLE "+qquest.getQid()+"_finish_notyet<br />\n");
						tsCode.append("!ADDRESOURCE #ship "+res.getId()+" -"+res.getCount1()+"<br />\n");
					}
				}
				tsCode.append("!COPY #ship 0<br />\n");
			}
			if( qquest.getReqRe() > 0 ) {
				tsCode.append("// RE ueberpruefen<br />\n");
				tsCode.append("!GETMONEY #USER<br />\n");
				tsCode.append("!COMPARE #A "+qquest.getReqRe()+"<br />\n");
				tsCode.append("!JL "+qquest.getQid()+"_finish_notyet<br />\n");
			}
			
			// Nun die Items/RE auch wirklich abbuchen...
			if( !qquest.getReqItems().isEmpty() ) {
				tsCode.append("// Resourcen abbuchen<br />\n");
				tsCode.append("!COPYVAR #ship shipsource.cargo<br />\n");
				ResourceList reslist = qquest.getReqItems().getResourceList();
				for( ResourceEntry res : reslist ) {
					if( res.getId().isItem() && (res.getId().getQuest() != 0) ) {
						tsCode.append("!ADDQUESTITEM #ship "+res.getId().getItemID()+" -"+res.getCount1()+"<br />\n");
					}
					else {
						tsCode.append("!ADDRESOURCE #ship "+res.getId()+" -"+res.getCount1()+"<br />\n");
					}
				}
				tsCode.append("!SAVEVAR shipsource.cargo #ship<br />\n");
				tsCode.append("!COPY #ship 0<br />\n");
			}
			if( qquest.getReqRe() > 0 ) {
				tsCode.append("#reqmoneytext = \"Kosten Quest \'"+qquest.getQName()+"\'\"<br />\n");
				tsCode.append("!ADDMONEY 0 #USER "+qquest.getReqRe()+" #reqmoneytext 0<br />\n");
			}
			
			// Belohnungen (Waren/RE)
			if( !qquest.getAwardItems().isEmpty() ) {
				tsCode.append("// Resourcen ueberpruefen<br />\n");
				tsCode.append("!COPYVAR #ship shipsource.cargo<br />\n");

				ResourceList reslist = qquest.getAwardItems().getResourceList();
				for( ResourceEntry res : reslist ) {
					tsCode.append("!ADDRESOURCE #ship "+res.getId()+" "+res.getCount1()+"<br />\n");
				}
				tsCode.append("!SAVEVAR shipsource.cargo #ship<br />\n");
				tsCode.append("!COPY #ship 0<br />\n");				
			}
			if( qquest.getAwardRe() != 0 ) {
				tsCode.append("#addmoneytext = \"Belohnung Quest \'"+qquest.getQName()+"\'\"<br />\n");
				tsCode.append("!ADDMONEY #USER 0 "+qquest.getAwardRe()+" #addmoneytext 1<br />\n");
			}
			tsCode.append("!COMPLETEQUEST #QUEST<br />\n");
			tsCode.append("!LOADDIALOG &lt;dialogid id=\"ready\" /&gt;<br />\n");
			tsCode.append("!ADDANSWER &lt;answerid id=\"gtu-posten-generic:ende\" /&gt; quit<br />\n");
			tsCode.append("!INITDIALOG<br />\n");
			tsCode.append("!ENDQUEST<br />\n");
			tsCode.append("!PAUSE<br />\n<br />\n");
			
			tsCode.append(":"+qquest.getQid()+"_finish_notyet<br />\n");
			tsCode.append("!LOADDIALOG &lt;dialogid id=\"notyet\" /&gt;<br />\n");
			tsCode.append("!ADDANSWER &lt;answerid id=\"gtu-posten-generic:ende\" /&gt; quit<br />\n");
			tsCode.append("!INITDIALOG<br />\n");
			tsCode.append("!PAUSE<br />\n<br />\n");
			
			/*
			 *
			 *	Jetzt kommt der Code fuer das Quellenobjekt
			 *
			 */
			
			if( qquest.getSourceType().equals("gtuposten") ) {
				echo.append("&lt;injectscript id=\"gtu-posten-"+sourceobjectid+":posten\"&gt;<br />\n");
			}
			else {
				echo.append("WARNUNG: unbekannter sourcetype '"+qquest.getSourceType()+"' - injectscript muss manuell eingefuegt werden<br />\n");	
			}
			echo.append("&lt;part id=\"parameters\"&gt;");
			echo.append(qquest.getQid()+" "+qquest.getQid()+"_yes");
			if( sourceobjectid == targetobjectid ) {
				echo.append(" "+tsParams);	
			}
			echo.append("&lt;/part&gt;<br />\n");
			
			// Menupart (Start)
			echo.append("&lt;part id=\"menu\"&gt;<br />\n");
			echo.append("// Quest bereits beendet? Dann zum naechsten Quest<br />\n");
			if( !qquest.getMoreThanOnce() ) {
				echo.append("!HASQUESTCOMPLETED &lt;questid id=\""+qquest.getQid()+"\" /&gt;<br />\n");
				echo.append("!JG 0_quest"+qquest.getQid()+"_endcont<br />\n");
			}
			
			if( qquest.getDependsOnQuests().length() > 0 ) {
				String[] dquests = StringUtils.split(qquest.getDependsOnQuests(), ';');
				for( int i=0; i < dquests.length; i++ ) {
					echo.append("!HASQUESTCOMPLETED &lt;questid id=\""+dquests[i]+"\" /&gt;<br />\n");
					echo.append("!JLE 0_quest"+qquest.getQid()+"_endcont<br />\n");	
				}	
			}
			
			echo.append("// Hat der Spieler das Quest bereits angenommen?<br />\n");
			echo.append("!LoadQuestContext &lt;questid id=\""+qquest.getQid()+"\" /&gt;<br />\n");
			echo.append("!COMPARE #QSTATUS 1<br />\n");
			echo.append("!JGE 0_quest"+qquest.getQid()+"_endcont<br />\n");
			echo.append("!ADDANSWER &lt;answerid id=\"startquest\" /&gt; "+qquest.getQid()+"<br />\n");
			echo.append("!JUMP 0_quest"+qquest.getQid()+"_endcont<br />\n");
			echo.append(":0_quest"+qquest.getQid()+"_endcont<br />\n<br />\n");
			if( sourceobjectid == targetobjectid ) {
				echo.append("<br />\n"+tsMenu);	
			}
			echo.append("&lt;/part&gt;<br /><br />\n");
					
			// Codepart (Start)
			echo.append("&lt;part id=\"code\"&gt;<br />\n");
			echo.append("// Auftrag "+qquest.getQid()+"<br />\n");
			echo.append(":"+qquest.getQid()+"<br />\n");
			echo.append("!LoadQuestContext &lt;questid id=\""+qquest.getQid()+"\" /&gt;<br />\n");
			echo.append("!GETQUESTID #QUEST<br />\n");
			echo.append("!COMPARE #A &lt;questid id=\""+qquest.getQid()+"\" /&gt;<br />\n");
			echo.append("!JE 0<br />\n");
			if( !qquest.getMoreThanOnce() ) {
				echo.append("!HASQUESTCOMPLETED &lt;questid id=\""+qquest.getQid()+"\" /&gt;<br />\n");
				echo.append("!JG 0<br />\n");
			}
			
			if( qquest.getDependsOnQuests().length() > 0 ) {
				String[] dquests = StringUtils.split(qquest.getDependsOnQuests(), ';');
				for( int i=0; i < dquests.length; i++ ) {
					echo.append("!HASQUESTCOMPLETED &lt;questid id=\""+dquests[i]+"\" /&gt;<br />\n");
					echo.append("!JLE 0<br />\n");	
				}	
			}
			echo.append("!LOADDIALOG &lt;dialogid id=\"info\" /&gt;<br />\n");
			echo.append("!ADDANSWER &lt;answerid id=\"yes\" /&gt; "+qquest.getQid()+"_yes<br />\n");
			echo.append("!ADDANSWER &lt;answerid id=\"no\" /&gt; 0<br />\n");
			echo.append("!INITDIALOG<br />\n");
			echo.append("!PAUSE<br />\n<br />\n");
			
			
			// Quest ist angenommen - also los
			echo.append(":"+qquest.getQid()+"_yes<br />\n");
			echo.append("!LoadQuestContext &lt;questid id=\""+qquest.getQid()+"\" /&gt;<br />\n");
			echo.append("!GETQUESTID #QUEST<br />\n");
			echo.append("!COMPARE #A &lt;questid id=\""+qquest.getQid()+"\" /&gt;<br />\n");
			echo.append("!JE 0<br />\n");
			if( !qquest.getMoreThanOnce() ) {
				echo.append("!HASQUESTCOMPLETED &lt;questid id=\""+qquest.getQid()+"\" /&gt;<br />\n");
				echo.append("!JG 0<br />\n");
			}
			
			if( qquest.getDependsOnQuests().length() > 0 ) {
				String[] dquests = StringUtils.split(qquest.getDependsOnQuests(), ';');
				for( int i=0; i < dquests.length; i++ ) {
					echo.append("!HASQUESTCOMPLETED &lt;questid id=\""+dquests[i]+"\" /&gt;<br />\n");
					echo.append("!JLE 0<br />\n");	
				}	
			}
			echo.append("!INITQUEST &lt;questid id=\""+qquest.getQid()+"\" /&gt;<br />\n");
			echo.append("!COPY #QSTATUS 1<br />\n");
				
			// Evt fuer das Quest benoetigte Items auf das Schiff transferieren
			if( !qquest.getStartItems().isEmpty() ) {
				echo.append("// Item transferieren<br />\n");
				echo.append("!COPYVAR #ship shipsource.cargo<br />\n");
				
				ResourceList reslist = qquest.getStartItems().getResourceList();
				for( ResourceEntry res : reslist ) {
					if( res.getId().isItem() && (res.getId().getQuest() != 0) ) {
						echo.append("!ADDQUESTITEM #ship "+res.getId().getItemID()+" "+res.getCount1()+"<br />\n");
					}
					else {
						echo.append("!ADDRESOURCE #ship "+res.getId()+" "+res.getCount1()+"<br />\n");	
					}
				}
				echo.append("!SAVEVAR shipsource.cargo #ship<br />\n");
				echo.append("!COPY #ship 0<br />\n");
			}
				
			// Loottable ergaenzen
			if( qquest.getLoottable().length() > 0 ) {
				String[] loottable = StringUtils.split(qquest.getLoottable(), ';');
				for( int i=0; i < loottable.length; i++ ) {
					String[] atable = StringUtils.split(loottable[i], ',');
					if( atable.length > 4 ) {
						echo.append("!ADDLOOTTABLE "+atable[0]+" "+atable[1]+" "+atable[2]+" "+atable[3]+" "+atable[4]);
						if( atable.length > 5 ) {
							echo.append(" "+atable[5]);
						}
						echo.append("<br />\n");	
					}	
				}	
			}
			echo.append("#quest"+qquest.getQid()+"_status=\""+qquest_shortdesc+"\"<br />\n");
			echo.append("!SETQUESTUISTATUS #quest"+qquest.getQid()+"_status 1<br />\n");
			echo.append("!PAUSE<br />\n<br />\n");
			if( sourceobjectid == targetobjectid ) {
				echo.append(tsCode);	
			}
			echo.append("&lt;/part&gt;<br />\n");
			echo.append("&lt;/injectscript&gt;<br />\n");
			
			/*
			 * Nun das Script fuer das Zielobjekt schreiben... 
			 */
			if( sourceobjectid != targetobjectid ) {
				if( qquest.getTargetType().equals("gtuposten") ) {
					echo.append("&lt;injectscript id=\"gtu-posten-"+targetobjectid+":posten\"&gt;<br />\n");
				}
				else {
					echo.append("WARNUNG: unbekannter targettype '"+qquest.getTargetType()+"' - injectscript muss manuell eingefuegt werden<br />\n");	
				}
				echo.append("&lt;part id=\"parameters\"&gt;"+tsParams+"&lt;/part&gt;<br />\n");
				echo.append("&lt;part id=\"menu\"&gt;<br />\n");
				echo.append(tsMenu);
				echo.append("&lt;/part&gt;<br />\n");
				echo.append("&lt;part id=\"code\"&gt;<br />\n");
				echo.append(tsCode);
				echo.append("&lt;/part&gt;<br />\n");
				echo.append("&lt;/injectscript&gt;<br />\n");
			}
			
			echo.append("&lt;/quest&gt;<br />\n");
			
			echo.append("</span>");
			echo.append(Common.tableEnd());
		}
	}
	
	private String nl2br(String str) {
		return StringUtils.replace(str, "\n", "<br />");
	}

}
