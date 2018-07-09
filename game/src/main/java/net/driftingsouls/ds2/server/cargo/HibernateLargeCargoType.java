package net.driftingsouls.ds2.server.cargo;

import java.sql.Types;

public class HibernateLargeCargoType extends HibernateCargoType
{
	@Override
	public int[] sqlTypes()
	{
		return new int[] {Types.CLOB};
	}
}
