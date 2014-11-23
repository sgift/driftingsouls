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

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Das Forschungszentrum.
 * @author Christopher Jung
 *
 */
@Entity(name="ForschungszentrumBuilding")
@DiscriminatorValue("net.driftingsouls.ds2.server.bases.ForschungszentrumBuilding")
public class ForschungszentrumBuilding extends DefaultBuilding {

	/**
	 * Erstellt eine neue Forschungszentrum-Instanz.
	 */
	public ForschungszentrumBuilding() {
		// EMPTY
	}

	@Override
	public void build(Base base, int building) {
		super.build(base, building);

		buildInternal(base);
	}

	private void buildInternal(Base base)
	{
		Context context = ContextMap.getContext();
		if( context == null ) {
			throw new RuntimeException("No Context available");
		}
		if( base.getForschungszentrum() == null )
		{
			org.hibernate.Session db = context.getDB();

			Forschungszentrum fz = new Forschungszentrum(base);
			db.persist(fz);

			base.setForschungszentrum(fz);
		}
	}

	@Override
	public void cleanup(Context context, Base base, int building) {
		super.cleanup(context, base, building);

		Forschungszentrum fz = base.getForschungszentrum();
		if( fz == null )
		{
			return;
		}
		base.setForschungszentrum(null);

		org.hibernate.Session db = context.getDB();
		db.delete(fz);
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
		Forschungszentrum fz = base.getForschungszentrum();
		if( (fz != null) && (fz.getDauer() > 0) ) {
			return true;
		}
		else if( fz == null ) {
			buildInternal(base);
		}
		return false;
	}

	@Override
	public String echoShortcut(Context context, Base base, int field, int building) {
		StringBuilder result = new StringBuilder(100);
		Forschungszentrum fz = base.getForschungszentrum();
		if( fz == null )
		{
			buildInternal(base);
			fz = base.getForschungszentrum();
		}

		if( fz.getDauer() == 0 ) {
			result.append("<a class=\"back tooltip\" href=\"./ds?module=building&amp;col=").append(base.getId()).append("&amp;field=").append(field).append("\">[F]<span class='ttcontent'>").append(this.getName()).append("</span></a>");
		}
		else {
			StringBuilder popup = new StringBuilder();
			popup.append(this.getName()).append(":<br />");
			Forschung forschung = fz.getForschung();
			popup.append("<img align='left' border='0' src='").append(fz.getForschung().getImage()).append("' alt='' />");
			popup.append(forschung.getName()).append("<br />");
			popup.append("Dauer: noch <img src='./data/interface/time.gif' alt='noch ' />").append(fz.getDauer()).append("<br />");

			result.append("<a class=\"error tooltip\" " + "href=\"./ds?module=building&amp;col=").append(base.getId()).append("&amp;field=").append(field).append("\">").append("[F]<span style=\"font-weight:normal\">").append(fz.getDauer()).append("</span>").append("<span class='ttcontent'>").append(popup).append("</span>").append("</a>");
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

		List<Integer> researches = new ArrayList<>();
		List<?> researchList = db.createQuery("from Forschungszentrum " +
				"where forschung is not null and base.owner=:owner")
				.setEntity("owner", user)
				.list();
		for (Object aResearchList : researchList)
		{
			Forschungszentrum aFz = (Forschungszentrum) aResearchList;

			if (aFz.getForschung() != null)
			{
				researches.add(aFz.getForschung().getID());
			}
		}

		boolean first = true;

		List<?> forschungen = db.createQuery("from Forschung order by name").list();
		for (Object aForschungen : forschungen)
		{
			Forschung tech = (Forschung) aForschungen;

			if (!Rassen.get().rasse(user.getRace()).isMemberIn(tech.getRace()))
			{
				continue;
			}
			if (researches.contains(tech.getID()))
			{
				continue;
			}
			if (user.hasResearched(tech))
			{
				continue;
			}

			if (user.getFreeSpecializationPoints() < tech.getSpecializationCosts())
			{
				continue;
			}

			boolean ok = true;

			if( !user.hasResearched(tech.getBenoetigteForschungen()) )
			{
				ok = false;
			}

			if (ok)
			{
				if (!first)
				{
					echo.append("<tr><td colspan=\"2\" class=\"noBorderX\"><hr style=\"height:1px; border:0px; background-color:#606060; color:#606060\" /></td></tr>\n");
				} else
				{
					first = false;
				}

				echo.append("<tr>\n");
				echo.append("<td class=\"noBorderX\" style=\"width:60%\">\n");
				if (!user.isNoob() || !tech.hasFlag(Forschung.FLAG_DROP_NOOB_PROTECTION))
				{
					echo.append("<a class=\"forschinfo\" href=\"./ds?module=building&amp;col=").append(base.getId()).append("&amp;field=").append(field).append("&amp;res=").append(tech.getID()).append("\">").append(Common._plaintitle(tech.getName())).append("</a>\n");
				}
				else
				{
					echo.append("<a class=\"forschinfo\" " + "href=\"javascript:DS.ask(" + "'Achtung!\\nWenn Sie diese Technologie erforschen verlieren sie den GCP-Schutz. Dies bedeutet, dass Sie sowohl angreifen als auch angegriffen werden k&ouml;nnen'," + "'./ds?module=building&amp;col=").append(base.getId()).append("&amp;field=").append(field).append("&amp;res=").append(tech.getID()).append("'").append(")\">").append(Common._plaintitle(tech.getName())).append("</a>\n");
				}
				echo.append("<a class=\"forschinfo\" href=\"./ds?module=forschinfo&amp;res=").append(tech.getID()).append("\"><img style=\"border:0px;vertical-align:middle\" src=\"./data/interface/forschung/info.gif\" alt=\"?\" /></a>\n");
				echo.append("&nbsp;&nbsp;");
				echo.append("</td>\n");

				echo.append("<td class=\"noBorderX\">");
				echo.append("<img style=\"vertical-align:middle\" src=\"./data/interface/time.gif\" alt=\"Dauer\" />").append(tech.getTime()).append(" ");
				echo.append("<img style=\"vertical-align:middle\" src=\"./data/interface/forschung/specpoints.gif\" alt=\"Spezialisierungskosten\" />").append(tech.getSpecializationCosts()).append(" ");

				Cargo costs = tech.getCosts();
				costs.setOption(Cargo.Option.SHOWMASS, false);

				ResourceList reslist = costs.compare(cargo, false, false, true);
				for (ResourceEntry res : reslist)
				{
					if (res.getDiff() > 0)
					{
						echo.append("<img style=\"vertical-align:middle\" src=\"").append(res.getImage()).append("\" alt=\"\" /><span style=\"color:red\">").append(res.getCargo1()).append("</span> ");
					} else
					{
						echo.append("<img style=\"vertical-align:middle\" src=\"").append(res.getImage()).append("\" alt=\"\" />").append(res.getCargo1()).append(" ");
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

			if( tech.isVisibile(user) && user.hasResearched(tech) ) {
				echo.append("<tr><td class=\"noBorderX\">\n");
				echo.append("<a class=\"forschinfo\" href=\"./ds?module=forschinfo&amp;res=").append(tech.getID()).append("\">").append(Common._plaintitle(tech.getName())).append("</a>");
				echo.append("</td><td class=\"noBorderX\"><img src=\"./data/interface/forschung/specpoints.gif\" alt=\"Spezialisierungskosten\">").append(tech.getSpecializationCosts()).append("</td>");
				echo.append("</tr>\n");
			}
		}
		echo.append("</table><br />");
	}

	private boolean currentResearch(Context context, StringBuilder echo, Forschungszentrum fz, int field ) {
		Forschung tech = fz.getForschung();
		if( tech != null ) {
			echo.append("<img style=\"float:left;border:0px\" src=\"").append(tech.getImage()).append("\" alt=\"\" />");
			echo.append("Erforscht: <a class=\"forschinfo\" href=\"./ds?module=forschinfo&amp;res=").append(tech.getID()).append("\">").append(Common._plaintitle(tech.getName())).append("</a>\n");
			echo.append("[<a class=\"error\" href=\"./ds?module=building&amp;col=").append(fz.getBase().getId()).append("&amp;field=").append(field).append("&amp;kill=yes\">x</a>]<br />\n");
			echo.append("Dauer: noch <img style=\"vertical-align:middle\" src=\"./data/interface/time.gif\" alt=\"\" />").append(fz.getDauer()).append(" Runden\n");
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
			echo.append("<a class=\"error\" href=\"./ds?module=building&amp;col=").append(fz.getBase().getId()).append("&amp;field=").append(field).append("&amp;kill=yes&amp;conf=ok\">Forschung abbrechen</a><br />\n");
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
			echo.append("<a class=\"error\" href=\"./ds?module=base&amp;col=").append(base.getId()).append("\">Fehler: Diese Forschung kann von ihrer Rasse nicht erforscht werden</a>\n");
			return;
		}

		Cargo techCosts = tech.getCosts();
		techCosts.setOption( Cargo.Option.SHOWMASS, false );

		// Muss der User die Forschung noch best?tigen?
		if( !conf.equals("ok") ) {
			echo.append("<div style=\"text-align:center\">\n");
			echo.append(Common._plaintitle(tech.getName())).append("<br /><img style=\"vertical-align:middle\" src=\"./data/interface/time.gif\" alt=\"Dauer\" />").append(tech.getTime()).append(" ");
			echo.append("<img style=\"vertical-align:middle\" src=\"./data/interface/forschung/specpoints.gif\" alt=\"Spezialisierungskosten\" />").append(tech.getSpecializationCosts()).append(" ");
			ResourceList reslist = techCosts.getResourceList();
			for( ResourceEntry res : reslist ) {
				echo.append("<img style=\"vertical-align:middle\" src=\"").append(res.getImage()).append("\" alt=\"\" />").append(res.getCargo1()).append(" ");
			}

			echo.append("<br /><br />\n");
			echo.append("<a class=\"ok\" href=\"./ds?module=building&amp;col=").append(base.getId()).append("&amp;field=").append(field).append("&amp;res=").append(researchid).append("&amp;conf=ok\">Erforschen</a></span><br />\n");
			echo.append("</div>\n");

			return;
		}

		// Wird bereits im Forschungszentrum geforscht?
		if( fz.getForschung() != null ) {
			ok = false;
		}

		// Besitzt der Spieler alle fuer die Forschung noetigen Forschungen?
		if( !user.hasResearched(tech.getBenoetigteForschungen()) )
		{
			ok = false;
		}

		if(user.getFreeSpecializationPoints() < tech.getSpecializationCosts())
		{
			ok = false;
		}

		if( !ok ) {
			echo.append("<a class=\"error\" href=\"./ds?module=base&amp;col=").append(base.getId()).append("\">Fehler: Forschung kann nicht durchgef&uuml;hrt werden</a>\n");
			return;
		}

		// Alles bis hierhin ok -> nun zu den Resourcen!
		Cargo cargo = new Cargo(base.getCargo());
		ok = true;

		ResourceList reslist = techCosts.compare( cargo, false, false, true );
		for( ResourceEntry res : reslist ) {
			if( res.getDiff() > 0 ) {
				echo.append("<span style=\"color:red\">Nicht genug <img style=\"vertical-align:middle\" src=\"").append(res.getImage()).append("\" alt=\"\" />").append(res.getName()).append("</span><br />\n");
				ok = false;
			}
		}

		// Alles OK -> Forschung starten!!!
		if( ok ) {
			cargo.substractCargo( techCosts );
			echo.append("<div style=\"text-align:center;color:green\">\n");
			echo.append(Common._plaintitle(tech.getName())).append(" wird erforscht<br />\n");
			echo.append("</div>\n");

			fz.setForschung(tech);
			fz.setDauer(tech.getTime());
			base.setCargo(cargo);
		}
	}

	@Override
	public String output(Context context, Base base, int field, int building) {

		int research = context.getRequest().getParameterInt("res");
		String confirm = context.getRequest().getParameterString("conf");
		String kill = context.getRequest().getParameterString("kill");
		String show = context.getRequest().getParameterString("show");
		if( !show.equals("oldres") ) {
			show = "newres";
		}

		StringBuilder echo = new StringBuilder(2000);

		Forschungszentrum fz = base.getForschungszentrum();
		if( fz == null ) {
			buildInternal(base);
			fz = base.getForschungszentrum();
		}

		echo.append("<table class=\"show\" cellspacing=\"2\" cellpadding=\"2\">\n");
		echo.append("<tr><td class=\"noBorderS\" style=\"text-align:center;font-size:12px\">Forschungszentrum<br />").append(base.getName()).append("</td><td class=\"noBorder\">&nbsp;</td>\n");

		//Neue Forschung & Bereits erforscht
		echo.append("<td class=\"noBorderS\">\n");

		echo.append("<div class='gfxbox' style='width:480px;text-align:center'>");

		echo.append("<a class=\"forschinfo\" href=\"./ds?module=building&amp;col=").append(base.getId()).append("&amp;field=").append(field).append("&amp;show=newres\">Neue Forschung</a>&nbsp;\n");
		echo.append("&nbsp;|&nbsp;&nbsp;<a class=\"forschinfo\" href=\"./ds?module=building&amp;col=").append(base.getId()).append("&amp;field=").append(field).append("&amp;show=oldres\">Bereits erforscht</a>\n");

		echo.append("</div>");

		echo.append("</td>\n");
		echo.append("</tr>\n");
		echo.append("<tr>\n");
		echo.append("<td colspan=\"3\" class=\"noBorderS\">\n");
		echo.append("<br />\n");

		echo.append("<div class='gfxbox' style='width:610px'>");

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

		echo.append("</div>");

		echo.append("<br />\n");
		echo.append("</td>\n");
		echo.append("</tr>\n");

		echo.append("</table>");
		return echo.toString();
	}

	@Override
	public boolean isSupportsJson()
	{
		return false;
	}
}
