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

import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.entities.Forschungszentrum;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

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

	@Override
	public String output(Context context, Base base, int field, int building) {
		return null;
	}

	@Override
	public boolean isSupportsJson()
	{
		return false;
	}
}
