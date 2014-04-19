package net.driftingsouls.ds2.server.modules.admin.editoren;

import javax.annotation.Nonnull;

/**
 * Adapter fuer {@link net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor}-Module
 * an das Admin-Pluginsystem.
 * @param <E> Der Typ der bearbeiteten Entity
 */
public class EditPlugin8<E> extends AbstractEditPlugin8<E>
{
	private EntityEditor<E> entityEditor;

	public EditPlugin8(EntityEditor<E> entityEditor)
	{
		super(entityEditor.getEntityType());
		this.entityEditor = entityEditor;
	}

	@Override
	protected void configureFor(@Nonnull EditorForm8<E> form)
	{
		this.entityEditor.configureFor(form);
	}

	@Override
	protected Class<?> getPluginClass()
	{
		return entityEditor.getClass();
	}
}
