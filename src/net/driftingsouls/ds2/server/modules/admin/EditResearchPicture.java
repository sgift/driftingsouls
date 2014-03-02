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

import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.framework.DynamicContentManager;

import java.io.IOException;

/**
 * Aktualisierungstool fuer Forschungsgrafiken.
 *
 * @author Christopher Jung
 */
@AdminMenuEntry(category = "Techs", name = "Forschungsgrafik editieren")
public class EditResearchPicture extends AbstractEditPlugin<Forschung> implements AdminPlugin
{
	public EditResearchPicture()
	{
		super(Forschung.class);
	}

	@Override
	protected void update(StatusWriter writer, Forschung forschung) throws IOException
	{
		String img = this.processDynamicContent("image", forschung.getImage());

		String oldImg = forschung.getImage();
		forschung.setImage("data/dynamicContent/"+img);

		if( oldImg.startsWith("data/dynamicContent/") )
		{
			DynamicContentManager.remove(oldImg);
		}
	}

	@Override
	protected void edit(EditorForm form, Forschung forschung)
	{
		form.editLabel("Name", forschung.getName());
		form.editDynamicContentField("Bild", "image", forschung.getImage());
	}
}
