package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.Nebel;
import org.springframework.stereotype.Service;

@Service
public class LocationService {
    private final NebulaService nebulaService;

    public LocationService(NebulaService nebulaService) {
        this.nebulaService = nebulaService;
    }

    /**
     * Gibt die angezeigten Koordinaten zurueck.
     * EMP wird dabei beruecksichtigt.
     *
     * @param noSystem <code>true</code>, wenn das System nicht mit angezeigt werden soll, sonst <code>false</code>
     * @return Anzeigbare Koordinaten.
     */
    public String displayCoordinates(Location location, boolean noSystem)
    {
        Nebel.Typ nebulaType = nebulaService.getNebula(location);

        StringBuilder text = new StringBuilder(8);
        if( !noSystem ) {
            text.append(location.getSystem());
            text.append(":");
        }

        if( nebulaType == Nebel.Typ.LOW_EMP ) {
            text.append(location.getX() / 10);
            text.append("x/");
            text.append(location.getY() / 10);
            text.append('x');

            return text.toString();
        }
        else if( (nebulaType == Nebel.Typ.MEDIUM_EMP) || (nebulaType == Nebel.Typ.STRONG_EMP) ) {
            text.append("??/??");
            return text.toString();
        }
        text.append(location.getX());
        text.append('/');
        text.append(location.getY());

        return text.toString();
    }

    /**
     * Gibt die fuer den Benutzer sichtbaren Koordinaten als URL-Fragment fuer die Sternenkarte zurueck
     * (z.B. <code>4/50/51</code> fuer die Position <code>4:50/51</code>).
     * EMP wird dabei beruecksichtigt.
     *
     * @return Das URL-Fragment.
     */
    public String urlFragment(Location location)
    {
        Nebel.Typ nebulaType = nebulaService.getNebula(location);

        StringBuilder text = new StringBuilder(8);
        text.append(location.getSystem());
        text.append("/");

        if( nebulaType == Nebel.Typ.LOW_EMP ) {
            text.append(location.getX() / 10);
            text.append("x/");
            text.append(location.getY() / 10);
            text.append('x');

            return text.toString();
        }
        else if( (nebulaType == Nebel.Typ.MEDIUM_EMP) || (nebulaType == Nebel.Typ.STRONG_EMP) ) {
            text.append("xx/xx");
            return text.toString();
        }
        text.append(location.getX());
        text.append('/');
        text.append(location.getY());

        return text.toString();
    }
}
