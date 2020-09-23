package net.driftingsouls.ds2.server.framework.db.batch;

import org.springframework.lang.NonNull;
import java.util.List;

/**
 * Interface zum Melden von Fehlern beim Verarbeiten von Arbeitsaufgaben.
 * Das Interface ist ein funktionales Interface und ist fuer die Verwendung als
 * Methodenreferenz oder Lambda gedacht.
 * @param <T> Der von der Arbeitsaufgabe konsumierte Objekttyp
 */
@FunctionalInterface
public interface UnitOfWorkErrorReporter<T>
{
	/**
	 * Meldet einen Fehler bei der Abarbeitung einer Arbeitsaufgabe. Kann pro Arbeitsaufgabe
	 * mehrfach aufgerufen werden.
	 *
	 * @param unitOfWork Die Arbeitsaufgabe
	 * @param failedObjects Die Fehlgeschlagenen Objekte
	 * @param error Die Fehlermeldung
	 */
    void report(@NonNull UnitOfWork<T> unitOfWork, @NonNull List<T> failedObjects, @NonNull Throwable error);
}
