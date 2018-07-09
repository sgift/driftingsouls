package net.driftingsouls.ds2.server.framework;

/**
 * Ein leerer {@link PermissionResolver}, der alle
 * Berechtigungspruefungen ablehnt.
 * @author christopherjung
 *
 */
public class EmptyPermissionResolver implements PermissionResolver
{
	@Override
	public boolean hasPermission(PermissionDescriptor permission)
	{
		return false;
	}
}
