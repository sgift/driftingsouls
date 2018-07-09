package net.driftingsouls.ds2.server.modules.admin.editoren;

/**
 * Verarbeitungsschritt einer nach einem Update auszufuehrenden Arbeitsaufgabe.
 * @param <E> Der Typ der aktulaisierten Entity
 * @param <T> Der Typ der Jobdaten fuer die der Verarbeitungsschritt durchzufuehren ist
 */
@FunctionalInterface
public interface PostUpdateTaskConsumer<E,T>
{
	/**
	 * Fuehrt den Verarbeitungsschritt aus.
	 * @param oldValue Die Entity mit den alten Werten vor der Aktualisierung
	 * @param currentValue Die Entity mit den neuen Werten nach der Aktualisierung
	 * @param jobData Der Jobdaten fuer den Verarbeitungsschritt
	 */
    void accept(E oldValue, E currentValue, T jobData);
}
