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

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.config.Medal;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.services.MedalService;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.entities.Rasse;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;

import net.driftingsouls.ds2.server.services.UserService;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Aktualisierungstool fuer die Werte eines Spielers.
 *
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Spieler", name = "Spieler", permission = WellKnownAdminPermission.EDIT_USER)
@Component
public class EditUser implements EntityEditor<User>
{
	private final BBCodeParser bbCodeParser;
	private final UserService userService;
	private final MedalService medalService;

	public EditUser(BBCodeParser bbCodeParser, UserService userService, MedalService medalService) {
		this.bbCodeParser = bbCodeParser;
		this.userService = userService;
		this.medalService = medalService;
	}

	@Override
	public Class<User> getEntityType()
	{
		return User.class;
	}

	@Override
	public void configureFor(@NonNull EditorForm8<User> form)
	{
		form.allowUpdate((u) -> u.getAccessLevel() <= ContextMap.getContext().getActiveUser().getAccessLevel());

		form.label("Loginname", User::getUN);
		form.field("Name", String.class, User::getNickname, this::updateName);
		form.field("Email", String.class, User::getEmail, User::setEmail);
		form.field("Accesslevel", Integer.class, User::getAccessLevel, (u, level) -> u.setAccesslevel(Math.min(level, ContextMap.getContext().getActiveUser().getAccessLevel())));
		form.field("Rasse", Rasse.class, Integer.class, User::getRace, User::setRace);
		form.field("Vacation", Integer.class, User::getVacationCount, User::setVacationCount);
		form.field("Wait4Vac", Integer.class, User::getWait4VacationCount, User::setWait4VacationCount);
		form.field("Konto", BigInteger.class, User::getKonto, User::setKonto);
		form.multiSelection("Flags", UserFlag.class, User::getFlags, User::setFlags)
				.withOptions(Arrays.stream(UserFlag.values()).collect(Collectors.toMap((f) -> f, UserFlag::getFlag)));
		form.field("Rang", Integer.class, User::getRang, User::setRang).withOptions(medalService.raenge());
		form.textArea("History", User::getHistory, User::setHistory);
		form.field("NPC-Punkte", Integer.class, User::getNpcPunkte, User::setNpcPunkte);
		form.multiSelection("Medaillen", Medal.class, userService::getMedals, User::setMedals)
				.withOptions(medalService.medals().stream().collect(Collectors.toMap(Medal::getId, Medal::getName)));
		form.field("Vac-Punkte", Integer.class, User::getVacpoints, User::setVacpoints);
		form.field("Spezialisierungspunkte", Integer.class, User::getSpecializationPoints, User::setSpecializationPoints);
		form.field("Zugang sperren", Boolean.class, User::getDisabled, User::setDisabled);
		form.multiSelection("Forschungen", Forschung.class, User::getForschungen, User::setForschungen);

		form.postUpdateTask("Vacation-Markierung setzen/entfernen", this::doVacation);
	}

	private void updateName(User user, String name)
	{
		user.setNickname(name);
		String newname = name;
		if( user.getAlly() != null ) {
			newname = user.getAlly().getAllyTag();
			newname = newname.replace("[name]", name);
		}
		user.setName(newname);
		user.setPlainname(bbCodeParser.parse(name,new String[] {"all"}));
	}

	private void doVacation(User orguser, User user)
	{
		if(user.getVacationCount() == 0)
		{
			user.setName(user.getName().replace(" [VAC]", ""));
			user.setPlainname(bbCodeParser.parse(user.getName().replace(" [VAC]", ""), new String[] {"all"}));
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
				user.setPlainname(bbCodeParser.parse(name+" [VAC]", new String[] {"all"}));
				user.setNickname(nickname+" [VAC]");
			}
			user.setInactivity(0);
		}
	}
}
