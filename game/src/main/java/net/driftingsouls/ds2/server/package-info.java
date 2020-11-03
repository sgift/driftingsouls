@TypeDefs(
    {
        @TypeDef(
            name="cargo",
            typeClass = HibernateCargoType.class
        ),
        @TypeDef(
            name="largeCargo",
            typeClass = HibernateLargeCargoType.class
        )
    }
)
package net.driftingsouls.ds2.server;

import net.driftingsouls.ds2.server.cargo.HibernateCargoType;
import net.driftingsouls.ds2.server.cargo.HibernateLargeCargoType;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;