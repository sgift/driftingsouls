package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.WellKnownUserValue;
import net.driftingsouls.ds2.server.framework.UserValue;
import net.driftingsouls.ds2.server.framework.utils.StringToTypeConverter;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A service to handle user values.
 *
 * Not part of the user service because of circular dependency problems.
 */
@Service
public class UserValueService {
    @PersistenceContext
    private EntityManager em;

    /**
     * Liefert den Wert einer Benutzereinstellung zurueck. Sofern mehrere Eintraege zu diesem
     * User-Value existieren wird der aelteste zurueckgegeben.
     *
     * @param valueDesc Die Beschreibung der Einstellung
     * @return Wert des User-Values
     */
    public <T> T getUserValue(User user, WellKnownUserValue<T> valueDesc ) {
        Optional<UserValue> value = em.createQuery("from UserValue where user=:user and name=:name order by id", UserValue.class)
            .setParameter("user", user)
            .setParameter("name", valueDesc.getName())
            .setMaxResults(1)
            .getResultStream().findAny();

        return value
            .map(val -> StringToTypeConverter.convert(valueDesc.getType(), val.getValue()))
            .orElse(StringToTypeConverter.convert(valueDesc.getType(), valueDesc.getDefaultValue()));
    }

    /**
     * Liefert alle Werte eines User-Values zurueck.
     * User-Values sind die Eintraege, welche sich in der Tabelle user_values befinden.
     *
     * @param valueDesc Die Beschreibung der Einstellung
     * @return Werte des User-Values
     */
    public <T> List<T> getUserValues(User user, WellKnownUserValue<T> valueDesc ) {
        List<UserValue> values = em.createQuery("from UserValue where user=:user and name=:name order by id", UserValue.class)
            .setParameter("user", user)
            .setParameter("name", valueDesc.getName())
            .getResultList();

        if( values.isEmpty() )
        {
            return Collections.singletonList(StringToTypeConverter.convert(valueDesc.getType(), valueDesc.getDefaultValue()));
        }

        return values.stream().map(UserValue::getValue).map(v -> StringToTypeConverter.convert(valueDesc.getType(), v)).collect(Collectors.toList());
    }

    /**
     * Setzt ein User-Value auf einen bestimmten Wert. Sollten mehrere Eintraege
     * existieren wird nur der aelteste aktualisiert.
     * @see #getUserValue(User, WellKnownUserValue)
     *
     * @param valueDesc Die Beschreibung der Einstellung
     * @param newvalue neuer Wert des User-Values
     */
    public <T> void setUserValue(User user, WellKnownUserValue<T> valueDesc, T newvalue ) {
        if(newvalue == null) {
            em.createQuery("delete from UserValue where user=:user and name=:name")
                .setParameter("user", this)
                .setParameter("name", valueDesc.getName())
                .executeUpdate();

            return;
        }

        Optional<UserValue> valuen = em.createQuery("from UserValue where user=:user and name=:name order by id", UserValue.class)
            .setParameter("user", user)
            .setParameter("name", valueDesc.getName())
            .getResultStream().findAny();

        valuen.ifPresentOrElse(userValue -> userValue.setValue(newvalue.toString()),
            () -> {
                var value = new UserValue(user, valueDesc.getName(), newvalue.toString());
                em.persist(value);
            });
    }

    /**
     * Gibt die ApiKey des Benutzers zurueck.
     * @return Die ApiKey
     */
    public String getApiKey(User user)
    {
        return getUserValue(user, WellKnownUserValue.APIKEY);
    }

    /**
     * Setzt die ApiKey des Benutzers.
     * @param ApiKey Die ApiKey
     */
    public void setApiKey(User user, String ApiKey)
    {
        setUserValue(user, WellKnownUserValue.APIKEY, ApiKey);
    }
}
