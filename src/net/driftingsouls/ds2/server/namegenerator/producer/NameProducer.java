package net.driftingsouls.ds2.server.namegenerator.producer;

/**
 * Interface fuer Produzenten von Namen. Jede implementierenden Klasse
 * muss Thread-Safe sein.
 */
public interface NameProducer
{
	/**
	 * Produziert den naechten Namen.
	 * @return Der Name
	 */
    String generateNext();
}
