package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.entities.Rasse;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;
import net.driftingsouls.ds2.server.namegenerator.PersonenNamenGenerator;
import net.driftingsouls.ds2.server.namegenerator.SchiffsKlassenNamenGenerator;
import net.driftingsouls.ds2.server.namegenerator.SchiffsNamenGenerator;

import javax.annotation.Nonnull;

@AdminMenuEntry(category = "Spieler", name = "Rasse", permission = WellKnownAdminPermission.EDIT_RASSE)
public class EditRasse implements EntityEditor<Rasse>
{
	@Override
	public Class<Rasse> getEntityType()
	{
		return Rasse.class;
	}

	@Override
	public void configureFor(@Nonnull EditorForm8<Rasse> form)
	{
		form.field("Name", String.class, Rasse::getName, Rasse::setName);
		form.field("Gehört zu", Rasse.class, Rasse::getMemberIn, Rasse::setMemberIn).withNullOption("[Keine]");
		form.field("Spielbar", Boolean.class, Rasse::isPlayable, Rasse::setPlayable);
		form.field("\"Erweitert\" Spielbar", Boolean.class, Rasse::isExtPlayable, Rasse::setExtPlayable);
		form.field("Oberhaupt", User.class, Rasse::getHead, Rasse::setHead).withNullOption("[Keiner]");
		form.field("Personennamengenerator", PersonenNamenGenerator.class, Rasse::getPersonenNamenGenerator, Rasse::setPersonenNamenGenerator);
		form.field("Schiffsklassengenerator", SchiffsKlassenNamenGenerator.class, Rasse::getSchiffsKlassenNamenGenerator, Rasse::setSchiffsKlassenNamenGenerator);
		form.field("Schiffsnamengenerator", SchiffsNamenGenerator.class, Rasse::getSchiffsNamenGenerator, Rasse::setSchiffsNamenGenerator);
		form.textArea("Beschreibung", Rasse::getDescription, Rasse::setDescription);
	}
}
