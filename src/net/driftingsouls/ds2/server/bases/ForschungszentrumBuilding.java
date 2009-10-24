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
package net.driftingsouls.ds2.server.bases;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.entities.Forschungszentrum;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Das Forschungszentrum.
 * @author Christopher Jung
 *
 */
@Entity(name="ForschungszentrumBuilding")
@DiscriminatorValue("net.driftingsouls.ds2.server.bases.ForschungszentrumBuilding")
@Configurable
public class ForschungszentrumBuilding extends DefaultBuilding {
	private static final Log log = LogFactory.getLog(ForschungszentrumBuilding.class);
	
	/**
	 * Erstellt eine neue Forschungszentrum-Instanz.
	 */
	public ForschungszentrumBuilding() {
		// EMPTY
	}

	@Override
	public void build(Base base) {
		super.build(base);
		
		Context context = ContextMap.getContext();
		if( context == null ) {
			throw new RuntimeException("No Context available");
		}
		org.hibernate.Session db = context.getDB();
		
		Forschungszentrum fz = new Forschungszentrum(base);
		db.persist(fz);
	}

	@Override
	public void cleanup(Context context, Base base) {
		super.cleanup(context, base);
		
		org.hibernate.Session db = context.getDB();
		db.createQuery("delete from Forschungszentrum where base=?")
			.setEntity(0, base)
			.executeUpdate();
	}
	
	@Override
	public boolean classicDesign() {
		return true;
	}

	@Override
	public boolean printHeader() {
		return false;
	}

	@Override
	public boolean isActive(Base base, int status, int field) {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		Forschungszentrum fz = (Forschungszentrum)db.get(Forschungszentrum.class, base.getId());
		if( (fz != null) && (fz.getDauer() > 0) ) {
			return true;
		}
		else if( fz == null ) {
			log.warn("Forschungszentrum ohne fz-Eintrag auf Basis "+base.getId()+" gefunden");
		}
		return false;
	}

	@Override
	public String echoShortcut(Context context, Base base, int field, int building) {
		org.hibernate.Session db = context.getDB();

		StringBuilder result = new StringBuilder(100);
		Forschungszentrum fz = (Forschungszentrum)db.get(Forschungszentrum.class, base.getId());
		if( fz != null ) {
			if( fz.getDauer() == 0 ) {
				result.append("<a class=\"back\" href=\"./ds?module=building&amp;col="+base.getId()+"&amp;field="+field+"\">[F]</a>");
			}
			else {
				StringBuilder popup = new StringBuilder(Common.tableBegin( 350, "left" ).replace("\"", "'") );
				Forschung forschung = fz.getForschung();
				popup.append("<img align='left' border='0' src='"+config.get("URL")+"data/tech/"+fz.getForschung().getID()+".gif' alt='' />");
				popup.append(forschung.getName()+"<br />");
				popup.append("Dauer: noch <img src='"+config.get("URL")+"data/interface/time.gif' alt='noch ' />"+fz.getDauer()+"<br />");
				popup.append( Common.tableEnd().replace("\"", "'") );

				result.append("<a name=\"p"+base.getId()+"_"+field+"\" id=\"p"+base.getId()+"_"+field+"\" " +
						"class=\"error\" " +
						"onmouseover=\"return overlib('<span style=\\'font-size:13px\\'>"+StringEscapeUtils.escapeJavaScript(popup.toString())+"</span>',REF,'p"+base.getId()+"_"+field+"',REFY,22,NOJUSTY,TIMEOUT,0,DELAY,150,WIDTH,280,BGCLASS,'gfxtooltip',FGCLASS,'gfxtooltip',TEXTFONTCLASS,'gfxtooltip');\" " +
						"onmouseout=\"return nd();\" " +
						"href=\"./ds?module=building&amp;col="+base.getId()+"&amp;field="+field+"\">[F]<span style=\"font-weight:normal\">"+fz.getDauer()+"</span></a>");
			}
		}
		else {
			result.append("WARNUNG: Forschungszentrum ohne Forschungszentrumeintrag gefunden<br />\n");
		}
		return result.toString();
	}
	
	private void possibleResearch(Context context, StringBuilder echo, Forschungszentrum fz, int field) {
		org.hibernate.Session db = context.getDB();

		User user = (User)context.getActiveUser();
	
		echo.append("M&ouml;gliche Forschungen: <br />\n");
		echo.append("<table class=\"noBorderX\" width=\"100%\">\n");
		echo.append("<tr>\n");
		echo.append("<td class=\"noBorderX\">Name</td>\n");
		echo.append("<td class=\"noBorderX\">Kosten</td>\n");
		echo.append("</tr>\n");
	
		Base base = fz.getBase();
		Cargo cargo = base.getCargo();
	
		List<Integer> researches = new ArrayList<Integer>();
		List<?> researchList = db.createQuery("from Forschungszentrum " +
				"where forschung is not null and base.owner=?")
				.setEntity(0, user)
				.list();
		for( Iterator<?> iter=researchList.iterator(); iter.hasNext(); ) {
			Forschungszentrum aFz = (Forschungszentrum)iter.next();
			
			if( aFz.getForschung() != null ) {
				researches.add(aFz.getForschung().getID());
			}
		}
		
		boolean first = true;
	
		List<?> forschungen = db.createQuery("from Forschung order by name").list();
		for( Iterator<?> iter=forschungen.iterator(); iter.hasNext(); ) {
			Forschung tech = (Forschung)iter.next();
			
			if( !Rassen.get().rasse(user.getRace()).isMemberIn(tech.getRace()) ) {
				continue;	
			}
			if( researches.contains(tech.getID()) ) {
				continue;
			}
			if( user.hasResearched(tech.getID())  ) {
				continue;
			}
			
			if(user.getFreeSpecializationPoints() < tech.getSpecializationCosts())
			{
				continue;
			}
			
			boolean ok = true;
			
			for( int k = 1; k <= 3; k++ ) {
				if( (tech.getRequiredResearch(k) != 0) && !user.hasResearched(tech.getRequiredResearch(k)) ) {
					ok = false;
				}
			}
			
			if( ok ) {
				if( !first ) {
					echo.append("<tr><td colspan=\"2\" class=\"noBorderX\"><hr style=\"height:1px; border:0px; background-color:#606060; color:#606060\" /></td></tr>\n");
				}
				else {
					first = false;
				}
				
				echo.append("<tr>\n");
				echo.append("<td class=\"noBorderX\" style=\"width:60%\">\n");
				if( !user.isNoob() || !tech.hasFlag(Forschung.FLAG_DROP_NOOB_PROTECTION) ) {
					echo.append("<a class=\"forschinfo\" href=\"./ds?module=building&amp;col="+base.getId()+"&amp;field="+field+"&amp;res="+tech.getID()+"\">"+Common._plaintitle(tech.getName())+"</a>\n");
				}
				else {
					echo.append("<a class=\"forschinfo\" " +
							"href=\"javascript:ask(" +
								"'Achtung!\\nWenn Sie diese Technologie erforschen verlieren sie den GCP-Schutz. Dies bedeutet, dass Sie sowohl angreifen als auch angegriffen werden k&ouml;nnen'," +
								"'./ds?module=building&amp;col="+base.getId()+"&amp;field="+field+"&amp;res="+tech.getID()+"'" +
							")\">"+Common._plaintitle(tech.getName())+"</a>\n");
				}
				echo.append("<a class=\"forschinfo\" href=\"./ds?module=forschinfo&amp;res="+tech.getID()+"\"><img style=\"border:0px;vertical-align:middle\" src=\""+config.get("URL")+"data/interface/forschung/info.gif\" alt=\"?\" /></a>\n");
				echo.append("&nbsp;&nbsp;");
				echo.append("</td>\n");
				
				echo.append("<td class=\"noBorderX\">");
				echo.append("<img style=\"vertical-align:middle\" src=\""+config.get("URL")+"data/interface/time.gif\" alt=\"Dauer\" />"+tech.getTime()+" ");
				echo.append("<img style=\"vertical-align:middle\" src=\""+config.get("URL")+"data/interface/forschung/specpoints.gif\" alt=\"Spezialisierungskosten\" />"+tech.getSpecializationCosts()+" ");
				
				Cargo costs = tech.getCosts();
				costs.setOption( Cargo.Option.SHOWMASS, false );
				
				ResourceList reslist = costs.compare( cargo, false, false, true );
				for( ResourceEntry res : reslist ) {
					if( res.getDiff() > 0 ) {
						echo.append("<img style=\"vertical-align:middle\" src=\""+res.getImage()+"\" alt=\"\" /><span style=\"color:red\">"+res.getCargo1()+"</span> ");
					}
					else {
						echo.append("<img style=\"vertical-align:middle\" src=\""+res.getImage()+"\" alt=\"\" />"+res.getCargo1()+" ");
					}
				}
	
				echo.append("</td></tr>\n");
			}
		}
	
		echo.append("</table><br />\n");
	}
	
	private void alreadyResearched( Context context, StringBuilder echo ) {
		org.hibernate.Session db = context.getDB();

		User user = (User)context.getActiveUser();
		
		echo.append("<table class=\"noBorderX\">");
		echo.append("<tr><td class=\"noBorderX\" align=\"left\">Bereits erforscht:</td></tr>\n");
	
		final Iterator<?> forschungIter = db.createQuery("from Forschung order by id").iterate();
		while( forschungIter.hasNext() ) {
			Forschung tech = (Forschung)forschungIter.next();
			
			if( tech.isVisibile(user) && user.hasResearched(tech.getID()) ) {
				echo.append("<tr><td class=\"noBorderX\">\n");
				echo.append("<a class=\"forschinfo\" href=\"./ds?module=forschinfo&amp;res="+tech.getID()+"\">"+Common._plaintitle(tech.getName())+"</a>");
				echo.append("</td><td class=\"noBorderX\"><img src=\""+config.get("URL")+"data/interface/forschung/specpoints.gif\" alt=\"Spezialisierungskosten\">"+tech.getSpecializationCosts()+"</td>");
				echo.append("</tr>\n");
			}
		}
		echo.append("</table><br />");
	}
	
	private boolean currentResearch(Context context, StringBuilder echo, Forschungszentrum fz, int field ) {
		Forschung tech = fz.getForschung();
		if( tech != null ) {
			echo.append("<img style=\"float:left;border:0px\" src=\""+config.get("URL")+"data/tech/"+tech.getID()+".gif\" alt=\"\" />");
			echo.append("Erforscht: <a class=\"forschinfo\" href=\"./ds?module=forschinfo&amp;res="+tech.getID()+"\">"+Common._plaintitle(tech.getName())+"</a>\n");
			echo.append("[<a class=\"error\" href=\"./ds?module=building&amp;col="+fz.getBase().getId()+"&amp;field="+field+"&amp;kill=yes\">x</a>]<br />\n");
			echo.append("Dauer: noch <img style=\"vertical-align:middle\" src=\""+config.get("URL")+"data/interface/time.gif\" alt=\"\" />"+fz.getDauer()+" Runden\n");
			echo.append("<br /><br />\n");
			return true;
		} 
		return false;
	}
	
	private void killResearch(Context context, StringBuilder echo, Forschungszentrum fz, int field, String conf) {
		if( !conf.equals("ok") ) {
			echo.append("<div style=\"text-align:center\">\n");
			echo.append("Wollen sie die Forschung wirklich abbrechen?<br />\n");
			echo.append("Achtung: Es erfolgt keine R&uuml;ckerstattung der Resourcen!<br /><br />\n");
			echo.append("<a class=\"error\" href=\"./ds?module=building&amp;col="+fz.getBase().getId()+"&amp;field="+field+"&amp;kill=yes&amp;conf=ok\">Forschung abbrechen</a><br />\n");
			echo.append("</div>\n");
			return;
		}
		
		fz.setForschung(null);
		fz.setDauer(0);
		
		echo.append("<div style=\"text-align:center;color:red;font-weight:bold\">\n");
		echo.append("Forschung abgebrochen<br />\n");
		echo.append("</div>");
	}
	
	private void doResearch(Context context, StringBuilder echo, Forschungszentrum fz, int researchid, int field, String conf) {
		User user = (User)context.getActiveUser();

		Base base = fz.getBase();
		
		Forschung tech = Forschung.getInstance(researchid);
		boolean ok = true;
	
		if( !Rassen.get().rasse(user.getRace()).isMemberIn(tech.getRace()) ) {
			echo.append("<a class=\"error\" href=\"./ds?module=base&amp;col="+base.getId()+"\">Fehler: Diese Forschung kann von ihrer Rasse nicht erforscht werden</a>\n");
			return;
		}
		
		Cargo techCosts = tech.getCosts();
		techCosts.setOption( Cargo.Option.SHOWMASS, false );
	
		// Muss der User die Forschung noch best?tigen?
		if( !conf.equals("ok") ) {
			echo.append("<div style=\"text-align:center\">\n");
			echo.append(Common._plaintitle(tech.getName())+"<br /><img style=\"vertical-align:middle\" src=\""+config.get("URL")+"data/interface/time.gif\" alt=\"Dauer\" />"+tech.getTime()+" ");
			echo.append("<img style=\"vertical-align:middle\" src=\""+config.get("URL")+"data/interface/forschung/specpoints.gif\" alt=\"Spezialisierungskosten\" />"+tech.getSpecializationCosts()+" ");
			ResourceList reslist = techCosts.getResourceList();
			for( ResourceEntry res : reslist ) {
				echo.append("<img style=\"vertical-align:middle\" src=\""+res.getImage()+"\" alt=\"\" />"+res.getCargo1()+" ");
			}
			
			echo.append("<br /><br />\n");
			echo.append("<a class=\"ok\" href=\"./ds?module=building&amp;col="+base.getId()+"&amp;field="+field+"&amp;res="+researchid+"&amp;conf=ok\">Erforschen</a></span><br />\n");
			echo.append("</div>\n");
			
			return;
		}
	
		// Wird bereits im Forschungszentrum geforscht?
		if( fz.getForschung() != null ) {
			ok = false;
		}
	
		// Besitzt der Spieler alle fuer die Forschung noetigen Forschungen?
		for( int i=1; i <= 3; i++ ) {
			if( !user.hasResearched(tech.getRequiredResearch(i)) ) {
				ok = false;
				break;
			}
		}
		
		if(user.getFreeSpecializationPoints() < tech.getSpecializationCosts())
		{
			ok = false;
		}
	
		if( !ok ) {
			echo.append("<a class=\"error\" href=\"./ds?module=base&amp;col="+base.getId()+"\">Fehler: Forschung kann nicht durchgef&uuml;hrt werden</a>\n");
			return;
		}
	
		// Alles bis hierhin ok -> nun zu den Resourcen!
		Cargo cargo = new Cargo(base.getCargo());
		ok = true;
		
		ResourceList reslist = techCosts.compare( cargo, false, false, true );
		for( ResourceEntry res : reslist ) {
			if( res.getDiff() > 0 ) {
				echo.append("<span style=\"color:red\">Nicht genug <img style=\"vertical-align:middle\" src=\""+res.getImage()+"\" alt=\"\" />"+res.getName()+"</span><br />\n");
				ok = false;
			}
		}
	
		// Alles OK -> Forschung starten!!!
		if( ok ) {
			cargo.substractCargo( techCosts );
			echo.append("<div style=\"text-align:center;color:green\">\n");
			echo.append(Common._plaintitle(tech.getName())+" wird erforscht<br />\n");
			echo.append("</div>\n");
			
			fz.setForschung(tech);
			fz.setDauer(tech.getTime());
			base.setCargo(cargo);
		}
	}

	@Override
	public String output(Context context, TemplateEngine t, Base base, int field, int building) {
		org.hibernate.Session db = context.getDB();

		int research 	= context.getRequest().getParameterInt("res");
		String confirm 	= context.getRequest().getParameterString("conf");
		String kill 		= context.getRequest().getParameterString("kill");
		String show 		= context.getRequest().getParameterString("show");
		if( !show.equals("oldres") ) {
			show = "newres";
		}
		
		StringBuilder echo = new StringBuilder(2000);
		
		Forschungszentrum fz = (Forschungszentrum)db.get(Forschungszentrum.class, base.getId());
		if( fz == null ) {
			echo.append("<span style=\"color:red\">Fehler: Dieses Forschungszentrum hat keinen Datenbank-Eintrag</span>\n");
			return echo.toString();
		}
		
		echo.append("<table class=\"show\" cellspacing=\"2\" cellpadding=\"2\">\n");
		echo.append("<tr><td class=\"noBorderS\" style=\"text-align:center;font-size:12px\">Forschungszentrum<br />"+base.getName()+"</td><td class=\"noBorder\">&nbsp;</td>\n");
		
		//Neue Forschung & Bereits erforscht
		echo.append("<td class=\"noBorderS\">\n");
	
		echo.append(Common.tableBegin( 440, "center" ));
		
		echo.append("<a class=\"forschinfo\" href=\"./ds?module=building&amp;col="+base.getId()+"&amp;field="+field+"&amp;show=newres\">Neue Forschung</a>&nbsp;\n");
		echo.append("&nbsp;|&nbsp;&nbsp;<a class=\"forschinfo\" href=\"./ds?module=building&amp;col="+base.getId()+"&amp;field="+field+"&amp;show=oldres\">Bereits erforscht</a>\n");
		
		echo.append(Common.tableEnd());

		echo.append("</td>\n");
		echo.append("</tr>\n");
		echo.append("<tr>\n");
		echo.append("<td colspan=\"3\" class=\"noBorderS\">\n");
		echo.append("<br />\n");
		
		echo.append(Common.tableBegin( 570, "left" ));
		
		if( (kill.length() != 0) || (research != 0) ) {
			if( kill.length() != 0 ) {
				killResearch( context, echo, fz, field, confirm);
			}
			if( research != 0 ) {
				doResearch( context, echo, fz, research, field, confirm );
			}
		}
		else if( show.equals("newres") ) {
			if( !currentResearch( context, echo, fz, field ) ) {
				possibleResearch( context, echo, fz, field );
			}
		} 
		else {
			alreadyResearched( context, echo );
		}
		
		echo.append(Common.tableEnd());

		echo.append("<br />\n");
		echo.append("</td>\n");
		echo.append("</tr>\n");
	
		echo.append("</table>");
		return echo.toString();
	}
}
