package net.driftingsouls.ds2.server.framework.authentication;

import net.driftingsouls.ds2.server.framework.PermissionDescriptor;
import net.driftingsouls.ds2.server.framework.PermissionResolver;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Ein PermissionResolver auf Basis des Accesslevels eines Benutzers.
 * @author christopherjung
 *
 */
public class AccessLevelPermissionResolver implements PermissionResolver, Serializable
{
	private static final long serialVersionUID = -5738261004354716068L;

	private int accessLevel;

	public AccessLevelPermissionResolver(int accessLevel)
	{
		this.accessLevel = accessLevel;
	}

	@Override
	public boolean hasPermission(PermissionDescriptor permission)
	{
		Map<String,Integer> perms = new HashMap<>();
		perms.put("admin#*", 30);
		perms.put("comm#adminPM", 30);
		perms.put("comm#offiziellePM", 30);
		perms.put("comnet#allesLesbar", 100);
		perms.put("comnet#allesSchreibbar", 100);
		perms.put("forschung#allesSichtbar", 20);
		perms.put("fraktionen#bietername", 20);
		perms.put("fraktionen#anbietername", 20);
		perms.put("handel#angeboteLoeschen", 20);
		perms.put("item#unbekannteSichtbar", 15);
		perms.put("item#modulSetMetaSichtbar", 15);
		perms.put("schiff#script", 20);
		perms.put("schiff#statusFeld", 30);
		perms.put("schiffstyp#npckostenSichtbar", 10);
		perms.put("schiffstyp#versteckteSichtbar", 10);
		perms.put("statistik#erweiterteSpielerliste", 20);
		perms.put("schlacht#liste", 20);
		perms.put("schlacht#alleAufrufbar", 20);
		perms.put("user#versteckteSichtbar", 20);
		perms.put("unit#versteckteSichtbar", 30);

		String category = permission.getCategory();
		String action = permission.getAction();

		if( perms.containsKey(category+"#"+action) )
		{
			return this.accessLevel >= perms.get(category+"#"+action);
		}
		if( perms.containsKey(category+"#*") )
		{
			return this.accessLevel >= perms.get(category+"#*");
		}
		throw new IllegalArgumentException("Unbekannte Permission: "+category+" "+action);
	}

}
