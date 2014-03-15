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

import net.driftingsouls.ds2.server.config.Medal;
import net.driftingsouls.ds2.server.config.Medals;
import net.driftingsouls.ds2.server.entities.Rasse;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;

/**
 * Aktualisierungstool fuer die Werte eines Spielers.
 *
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Spieler", name = "Spieler editieren")
public class EditUser extends AbstractEditPlugin<User>
{
	public EditUser()
	{
		super(User.class);
	}

	@Override
	protected void update(StatusWriter writer, User user) throws IOException
	{
		Context context = ContextMap.getContext();
		if( user.getAccessLevel() > context.getActiveUser().getAccessLevel() )
		{
			writer.append("Keine Berechtigung zum Bearbeiten dieses Benutzers");
			return;
		}

		boolean disableAccount = "true".equals(context.getRequest().getParameterString("blockuser"));

		user.setDisabled(disableAccount);

		String name = context.getRequest().getParameterString("name");
		user.setNickname(name);
		String newname = name;
		if( user.getAlly() != null ) {
			newname = user.getAlly().getAllyTag();
			newname = StringUtils.replace(newname, "[name]", name);
		}
		user.setName(newname);


		user.setRace(context.getRequest().getParameterInt("race"));
		user.setVacationCount(context.getRequest().getParameterInt("vacation"));
		user.setWait4VacationCount(context.getRequest().getParameterInt("wait4vac"));
		user.setKonto(new BigInteger(context.getRequest().getParameterString("account")));
		user.setFlags(context.getRequest().getParameterString("flags"));
		user.setRang(Byte.valueOf(context.getRequest().getParameterString("rank")));
		user.setHistory(context.getRequest().getParameterString("history"));
		user.setNpcPunkte(context.getRequest().getParameterInt("npcpoints"));
		user.setMedals(context.getRequest().getParameterString("medals"));
		user.setVacpoints(context.getRequest().getParameterInt("vacationpoints"));
		user.setSpecializationPoints(context.getRequest().getParameterInt("specializationpoints"));
		user.setEmail(context.getRequest().getParameterString("email"));

		int accesslevel = context.getRequest().getParameterInt("accesslevel");
		if( accesslevel > context.getActiveUser().getAccessLevel() )
		{
			accesslevel = context.getActiveUser().getAccessLevel();
		}
		user.setAccesslevel(accesslevel);

		doVacation(user);
	}

	@Override
	protected void edit(EditorForm form, User user)
	{
		form.label("Loginname", user.getUN());
		form.field("Name", "name", String.class, user.getNickname());
		form.field("Email", "email", String.class, user.getEmail());
		form.field("Accesslevel", "accesslevel", Integer.class, user.getAccessLevel());
		form.field("Rasse", "race", Rasse.class, user.getRace());
		form.field("Vacation", "vacation", Integer.class, user.getVacationCount());
		form.field("Wait4Vac", "wait4vac", Integer.class, user.getWait4VacationCount());
		form.field("Konto", "account", BigInteger.class, user.getKonto());
		form.field("Flags", "flags", String.class, user.getFlags());
		form.field("Rang", "rank", Integer.class, user.getRang()).withOptions(Medals.get().raenge());
		form.textArea("History", "history", user.getHistory());
		form.field("NPC-Punkte", "npcpoints", Integer.class, user.getNpcPunkte());
		form.field("Medaillen", "medals", String.class, user.getMedals());
		form.field("Vac-Punkte", "vacationpoints", Integer.class, user.getVacpoints());
		form.field("Spezialisierungspunkte", "specializationpoints", Integer.class, user.getSpecializationPoints());
		form.field("Zugang sperren", "blockuser", Boolean.class, user.getDisabled());

		StringBuilder echo = new StringBuilder();
		for(Map.Entry<Integer, Medal> medal: Medals.get().medals().entrySet())
		{
			echo.append(medal.getValue().getID()).append("=").append(medal.getValue().getName()).append(", ");
		}
		form.label("Vorhandene Medallien", echo);
	}

	private void doVacation(User user)
	{
		if(user.getVacationCount() == 0)
		{
			user.setName(user.getName().replace(" [VAC]", ""));
			user.setNickname(user.getNickname().replace(" [VAC]", ""));
		}
		else if(user.getWait4VacationCount() == 0)
		{
			if( !user.getName().contains("[VAC]") )
			{
				//Code geklaut aus RestTick - ueberarbeiten
				String name = user.getName();
				String nickname = user.getNickname();

				if( name.length() > 249 ) {
					name = name.substring(0, 249);
				}
				if( nickname.length() > 249 ) {
					nickname = nickname.substring(0, 249);
				}

				user.setName(name+" [VAC]");
				user.setNickname(nickname+" [VAC]");
			}
			user.setInactivity(0);
		}
	}
}
