/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
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
import java.util.List;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.DynamicContentManager;
import net.driftingsouls.ds2.server.modules.AdminController;

/**
 * Aktualisierungstool fuer Forschungsgrafiken.
 *
 * @author Christopher Jung
 */
@AdminMenuEntry(category = "Techs", name = "Forschungsgrafik editieren")
public class EditResearchPicture extends AbstractEditPlugin implements AdminPlugin
{
	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();

		int forschungid = context.getRequest().getParameterInt("entityId");

		this.beginSelectionBox(echo, page, action);
		List<Forschung> forschungen = Common.cast(db.createQuery("from Forschung order by id").list());
		for( Forschung f : forschungen )
		{
			this.addSelectionOption(echo, f.getID(), f.getName()+" ("+f.getID()+")");
		}
		this.endSelectionBox(echo);

		if(this.isUpdateExecuted() && forschungid != 0)
		{
			Forschung forschung = (Forschung)db.get(Forschung.class, forschungid);

			if(forschung != null) {
				String img = this.processDynamicContent("image", forschung.getImage());

				String oldImg = forschung.getImage();
				forschung.setImage("data/dynamicContent/"+img);

				if( oldImg.startsWith("data/dynamicContent/") )
				{
					DynamicContentManager.remove(oldImg);
				}

				echo.append("<p>Update abgeschlossen.</p>");
			}
			else {
				echo.append("<p>Keine Forschung gefunden.</p>");
			}

		}

		if(forschungid != 0)
		{
			Forschung forschung = (Forschung)db.get(Forschung.class, forschungid);

			if(forschung == null)
			{
				return;
			}

			this.beginEditorTable(echo, page, action, forschung.getID());
			this.editLabel(echo, "Name", forschung.getName());
			this.editDynamicContentField(echo, "Bild", "image", forschung.getImage());
			this.endEditorTable(echo);
		}
	}
}
