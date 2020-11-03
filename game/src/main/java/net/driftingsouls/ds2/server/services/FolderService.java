package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.comm.Ordner;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static net.driftingsouls.ds2.server.comm.Ordner.FLAG_TRASH;

@Service
public class FolderService {
    @PersistenceContext
    private EntityManager em;

    /**
     * Loescht den Ordner. Alle im Ordner enthaltenen.
     * Unterordner und Pms werden ebenfalls geloescht.
     * @return <code>0</code>, falls das Loeschen erfolgreich war, <code>1</code>, falls erst noch eine PM gelesen werden muss
     * und <code>2</code>, bei sonstigen Fehlern
     */
    public int deleteOrdner(Ordner folder) {
        int result;
        if( (folder.getFlags() & FLAG_TRASH) != 0 ) {
            return 2;
        }
        if( (result = deleteAllInOrdner( folder, folder.getOwner() )) != 0 ){
            return result;
        }
        List<Ordner> subFolders = em.createQuery("from Ordner where parent=:parent and owner=:owner", Ordner.class)
            .setParameter("parent", folder.getId())
            .setParameter("owner", folder.getOwner())
            .getResultList();

        for (Ordner subFolder: subFolders)
        {
            if ((result = deleteOrdner(subFolder)) != 0)
            {
                return result;
            }
        }

        em.remove(this);

        return 0;
    }

    /**
     * Gibt den Ordner mit der angegebenen ID zurueck.
     * Sollte kein solcher Ordner existieren, so wird <code>null</code> zurueckgegeben.
     *
     * @param id Die ID des Ordners
     * @return Der Ordner
     */
    public Ordner getOrdnerByID( int id ) {
        return getOrdnerByID(id, null);
    }

    /**
     * Gibt den Ordner mit der angegebenen ID des angegebenen Benutzers zurueck.
     * Sollte kein solcher Ordner existieren, so wird <code>null</code> zurueckgegeben.
     *
     * @param id Die ID des Ordners
     * @param user Der Benutzer
     * @return Der Ordner
     */
    public Ordner getOrdnerByID( int id, User user ) {
        if( id != 0 ) {
            Ordner ordner = em.find(Ordner.class, id);

            if( ordner == null ) {
                return null;
            }

            if( (user != null) && (ordner.getOwner() != user) ) {
                return null;
            }

            return ordner;
        }

        Ordner ordner = new Ordner();
        ordner.setId(0);
        ordner.setName("Hauptverzeichnis");
        ordner.setFlags(0);
        ordner.setOwner(user);

        return ordner;
    }

    /**
     * Gibt die Anzahl der PMs in allen Ordnern unterhalb des Ordners zurueck.
     * PMs in Unterordnern erhoehen die Anzahl der PMs im uebergeordneten Ordner.
     * Zurueckgegeben wird eine Map, in der die Ordner-ID der Schluessel ist. Der Wert
     * ist die Anzahl der PMs.
     * @return Map mit der Anzahl der PMs in den jeweiligen Unterordnern
     */
    public Map<Ordner,Integer> getPmCountPerSubOrdner(Ordner folder) {
        Map<Ordner,Integer> result = new HashMap<>();

        List<Ordner> ordners = getAllChildren(folder);

        // Wenn der Ordner keine Unterordner hat, dann eine leere Map zurueckgeben
        if( ordners.size() == 0 ) {
            return result;
        }

        // Array mit den Ordner-IDs erstellen sowie vermerken, wieviele Kindordner
        // ein Ordner besitzt
        Map<Ordner,Integer> childCount = new HashMap<>();
        Integer[] ordnerIDs = new Integer[ordners.size()];
        for( int i=0; i < ordners.size(); i++ ) {
            ordnerIDs[i] = ordners.get(i).getId();
            Common.safeIntInc(childCount, getOrdnerByID(ordners.get(i).getParent(), ordners.get(i).getOwner()));
        }

        // Map mit der Anzahl der PMs fuellen, die sich direkt im Ordner befinden
        Ordner trashCan = getTrash(folder.getOwner());

        List<Object[]> pmcounts = em.createQuery("select ordner, count(*) from PM " +
            "where empfaenger=:owner and ordner in :ordnerIds " +
            "and gelesen < case when ordner=:trash then 10 else 2 end " +
            "group by ordner", Object[].class)
            .setParameter("owner", folder.getOwner())
            .setParameter("ordnerIds", ordnerIDs)
            .setParameter("trash", trashCan.getId())
            .getResultList();
        for (Object[] pmcount : pmcounts)
        {
            result.put(getOrdnerByID(((Number) pmcount[0]).intValue()), ((Number) pmcount[1]).intValue());
        }


        // PMs in den einzelnen Ordnern unter Beruecksichtigung der
        // Unterordner berechnen - maximal 100 Zyklen lang
        int maxloops = 100;
        while( (childCount.size() > 0) && (maxloops-- > 0) ) {
            for( int i=0; i < ordners.size(); i++ ) {
                Ordner aOrdner = ordners.get(i);
                if( childCount.get(aOrdner) != null ) {
                    continue;
                }

                Ordner parent = getOrdnerByID(aOrdner.getParent());
                // Die Anzahl der PMs des Elternordners um die
                // des aktuellen Ordners erhoehen. Anschliessend
                // den aktuellen Ordner aus der Liste entfernen
                if( !result.containsKey(parent) ) {
                    result.put(parent, 0);
                }
                Integer child = result.get(aOrdner);
                result.put(parent, result.get(parent)+ (child != null ? child : 0) );

                childCount.put(parent, childCount.get(parent)-1);
                childCount.remove(aOrdner);

                ordners.remove(i);
                i--;
            }
        }

        return result;
    }

    /**
     * Gibt den Papierkorb eines Benutzers zurueck. Jeder Benutzer hat einen Papierkorb...
     * @param user Der Benutzer
     * @return Der Papierkorb
     */
    public Ordner getTrash(User user) {
        return em.createQuery("from Ordner where owner=:owner and bit_and(flags,:flag)!=0", Ordner.class)
            .setParameter("owner", user)
            .setParameter("flag", FLAG_TRASH)
            .getSingleResult();
    }

    /**
     * Loescht alle PMs im Ordner, die von dem angegebenen Benutzer stammen
     * (ausser solche, welche als wichtig markiert sind, aber noch
     * nicht gelesen wurden).
     * @param user Der Benutzer
     */
    public void deletePmsByUser(Ordner folder, User user) {
        Ordner trash = getTrash(folder.getOwner());


        em.createQuery("update PM set gelesen=2, ordner= :trash " +
            "where empfaenger= :user and sender=:sender and ordner= :ordner and (gelesen=1 or bit_and(flags,:important)=0)")
            .setParameter("trash", trash)
            .setParameter("user", folder.getOwner())
            .setParameter("sender", user)
            .setParameter("ordner", folder.getId())
            .setParameter("important", PM.FLAGS_IMPORTANT)
            .executeUpdate();
    }

    /**
     * Loescht alle Pms im Ordner (ausser solche, welche als wichtig markiert sind, aber noch
     * nicht gelesen wurden).
     *
     */
    public void deleteAllPms(Ordner folder) {
        Ordner trash = getTrash(folder.getOwner());

        em.createQuery("update PM set gelesen=2, ordner= :trash " +
            "where empfaenger= :user and ordner= :ordner and (gelesen=1 or bit_and(flags,:important)=0)")
            .setParameter("trash", trash.getId())
            .setParameter("user", folder.getOwner())
            .setParameter("ordner", folder.getId())
            .setParameter("important", PM.FLAGS_IMPORTANT)
            .executeUpdate();
    }

    /**
     * Markiert alle Pms im Ordner als gelesen (ausser solche, welche als wichtig markiert sind).
     *
     */
    public void markAllAsRead(Ordner folder) {
        em.createQuery("update PM set gelesen=1 " +
            "where empfaenger= :user and ordner= :ordner and (gelesen=0 and bit_and(flags,:important)=0)")
            .setParameter("user", folder.getOwner())
            .setParameter("ordner", folder.getId())
            .setParameter("important", PM.FLAGS_IMPORTANT)
            .executeUpdate();
    }

    /**
     * Gibt die Anzahl der im Ordner vorhandenen PMs zurueck.
     * Unterordner werden nicht beruecksichtigt.
     * @return Die Anzahl der PMs
     */
    public int getPmCount(Ordner folder) {
        int gelesen = 2;
        if( (folder.getFlags() & FLAG_TRASH) != 0 ){
            gelesen = 10;
        }

        return em.createQuery("select count(*) from PM where empfaenger=:owner and ordner=:ordner and gelesen<:read", Integer.class)
            .setParameter("owner", folder.getOwner())
            .setParameter("ordner", folder.getId())
            .setParameter("read", gelesen)
            .getSingleResult();
    }

    /**
     * Gibt alle PMs im Ordner selbst zurueck. PMs in unterordnern werden ignoriert.
     * @return Die Liste aller PMs im Ordner
     */
    public List<PM> getPms(Ordner folder) {
        return em.createQuery("from PM where empfaenger=:user and gelesen < :gelesen and ordner= :ordner order by time desc", PM.class)
            .setParameter("user", folder.getOwner())
            .setParameter("ordner", folder.getId())
            .setParameter("gelesen", folder.hasFlag(FLAG_TRASH) ? 10 : 2)
            .getResultList();
    }

    /**
     * Gibt alle direkten Kindordner des Ordners zurueck.
     * @return Liste mit Ordnern
     */
    public List<Ordner> getChildren(Ordner folder) {
        return em.createQuery("from Ordner where parent=:parent and owner=:owner", Ordner.class)
            .setParameter("parent", folder.getId())
            .setParameter("owner", folder.getOwner())
            .getResultList();
    }

    /**
     * Gibt alle Kindordner, ob direkt oder indirekt, des Ordners zurueck.
     * @return Liste mit Ordnern
     */
    public List<Ordner> getAllChildren(Ordner folder) {
        List<Ordner> children = getChildren(folder);
        List<Ordner> allChildren = new ArrayList<>(children);

        allChildren
            .addAll(children.stream()
                .flatMap(child -> getAllChildren(child).stream()).collect(Collectors.toList()));

        return allChildren;
    }

    /**
     * Erstellt einen neuen Ordner fuer einen bestimmten Spieler.
     * @param name Der Name des neuen Ordners
     * @param parent Der Elternordner
     * @param user Der Besitzer
     * @return Der neue Ordner
     * @throws IllegalArgumentException Falls der Elternordner der Papierkorb ist
     */
    public Ordner createNewOrdner( String name, Ordner parent, User user ) throws IllegalArgumentException {
        if( (parent.getFlags() & Ordner.FLAG_TRASH) != 0 ) {
            throw new IllegalArgumentException("Ordnererstellung im Papierkorb nicht moeglich");
        }
        Ordner ordner = new Ordner(name, user, parent);
        em.persist(ordner);

        return ordner;
    }

    /**
     * Gibt den Eltern-Ordner zurueck.
     * @return Der Elternordner
     */
    public Ordner getParent(Ordner folder) {
        return getOrdnerByID(folder.getParent(), folder.getOwner());
    }

    /**
     * Stelllt alle geloeschten PMs eines Spielers wieder her.
     * @param user Der Spieler
     */
    public void recoverAll( User user ) {
        int trash = getTrash( user ).getId();

        em.createQuery("update PM set ordner=0,gelesen=1 where ordner=:ordner")
            .setParameter("ordner", trash)
            .executeUpdate();
    }

    /**
     * Verschiebt alle PMs von einem Ordner in einen anderen.
     * @param source Der Ausgangsordner
     * @param dest Der Zielordner
     * @param user Der Besitzer der PM
     */
    public void moveAllToOrdner( Ordner source, Ordner dest , User user) {
        Ordner trash = getTrash( user );

        List<PM> pms = em.createQuery("from PM where ordner=:ordner and empfaenger=:user", PM.class)
            .setParameter("ordner", source.getId())
            .setParameter("user", user)
            .getResultList();
        for (PM pm: pms)
        {
            int gelesen = (trash == source) ? 1 : pm.getGelesen();

            pm.setGelesen((trash == dest) ? 2 : gelesen);
            pm.setOrdner(dest.getId());
        }
    }

    /**
     * Stellt eine geloeschte PM wieder her.
     */
    public void recover(PM pm) {
        if( pm.getGelesen() <= 1 ) {
            return;
        }

        int trash = getTrash(pm.getEmpfaenger()).getId();
        if( pm.getOrdner() != trash ) {
            return;
        }
        pm.setGelesen(1);
        pm.setOrdner(0);
    }

    /**
     * Loescht die PM.
     * @return 0, falls der Vorgang erfolgreich war. 1, wenn ein Fehler aufgetreten ist und 2, falls nicht alle PMs gelesen wurden
     */
    public int delete(PM pm) {
        if( pm.getGelesen() > 1 ) {
            return 2;
        }

        Ordner trashCan = getTrash(pm.getEmpfaenger());
        int trash = trashCan.getId();
        if( pm.hasFlag(PM.FLAGS_IMPORTANT) && (pm.getGelesen() < 1) ) {
            return 1;
        }
        pm.setGelesen(2);
        pm.setOrdner(trash);

        return 0;	//geloescht
    }

    /**
     * Loescht alle PMs aus einem Ordner eines bestimmten Spielers.
     * Der Vorgang schlaegt fehl, wenn noch nicht alle wichtigen PMs gelesen wurden.
     * @param ordner Der Ordner, dessen Inhalt geloescht werden soll
     * @param user Der Besitzer des Ordners
     * @return 0, falls der Vorgang erfolgreich war. 1, wenn ein Fehler aufgetreten ist und 2, falls nicht alle PMs gelesen wurden
     */
    public int deleteAllInOrdner( Ordner ordner, User user ) {
        List<PM> pms = em.createQuery("from PM where ordner=:ordner", PM.class)
            .setParameter("ordner", ordner.getId())
            .getResultList();
        for (PM pm : pms)
        {
            if (pm.getEmpfaenger().getId() != user.getId())
            {
                return 2;
            }
            int result = delete(pm);
            if (result != 0)
            {
                return result;
            }
        }

        return 0;	//geloescht
    }
}
