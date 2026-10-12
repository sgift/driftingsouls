package net.driftingsouls.ds2.server.modules.admin.editors;

import javax.annotation.Nonnull;

/**
 * Interface fuer Entity-Editoren des Adminsystems.
 * @param <E> Der Typ der Entity
 */
public interface EntityEditor<E>
{
	Class<E> getEntityType();
	void configureFor(@Nonnull EditorForm8<E> form);
}
