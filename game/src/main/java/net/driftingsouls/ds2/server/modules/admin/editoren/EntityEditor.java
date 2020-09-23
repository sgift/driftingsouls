package net.driftingsouls.ds2.server.modules.admin.editoren;

import org.springframework.lang.NonNull;

/**
 * Interface fuer Entity-Editoren des Adminsystems.
 * @param <E> Der Typ der Entity
 */
public interface EntityEditor<E>
{
	Class<E> getEntityType();
	void configureFor(@NonNull EditorForm8<E> form);
}
