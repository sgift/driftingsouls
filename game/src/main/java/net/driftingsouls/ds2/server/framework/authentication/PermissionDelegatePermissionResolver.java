package net.driftingsouls.ds2.server.framework.authentication;

import net.driftingsouls.ds2.server.framework.Permission;
import net.driftingsouls.ds2.server.framework.PermissionDescriptor;
import net.driftingsouls.ds2.server.framework.PermissionResolver;

import java.util.Set;

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
	public boolean hasPermission(PermissionDescriptor permission)
	{
		if( permission == null )
		{
			throw new IllegalArgumentException("permission darf nicht null sein");
		}

		for( Permission p : this.permissions )
		{
			if( permission.getCategory().equals(p.getCategory()) )
			{
				if( "*".equals(p.getAction()) || permission.getAction().equals(p.getAction()) )
				{
					return true;
				}
			}
		}
		return this.inner.hasPermission(permission);
	}

}
