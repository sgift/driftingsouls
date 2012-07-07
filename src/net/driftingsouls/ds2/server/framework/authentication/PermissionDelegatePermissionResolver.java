package net.driftingsouls.ds2.server.framework.authentication;

import java.util.Set;

import net.driftingsouls.ds2.server.framework.Permission;
import net.driftingsouls.ds2.server.framework.PermissionResolver;

/**
 * PermissionResolver auf Basis eines expliziten Permission-Satzes ({@link Permission}) mit Fallback.
 * Falls der Satz keine entsprechende Permission enthaelt erfolgt ein Fallback
 * auf einen anderen PermissionResolver.
 * @author christopherjung
 *
 */
public class PermissionDelegatePermissionResolver implements PermissionResolver
{
	private PermissionResolver inner;
	private Set<Permission> permissions;

	/**
	 * Konstruktor.
	 * @param inner Der alternative PermissionResolver (Fallback)
	 * @param permissions Die expliziten Permissions
	 */
	public PermissionDelegatePermissionResolver(PermissionResolver inner, Set<Permission> permissions)
	{
		super();
		this.inner = inner;
		this.permissions = permissions;
	}

	@Override
	public boolean hasPermission(String category, String action)
	{
		if( category == null || action == null )
		{
			throw new IllegalArgumentException("category und action duerfen nicht null sein");
		}

		for( Permission p : this.permissions )
		{
			if( category.equals(p.getCategory()) )
			{
				if( "*".equals(p.getAction()) || action.equals(p.getAction()) )
				{
					return true;
				}
			}
		}
		return this.inner.hasPermission(category, action);
	}

}
