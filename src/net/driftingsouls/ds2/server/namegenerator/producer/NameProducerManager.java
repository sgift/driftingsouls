package net.driftingsouls.ds2.server.namegenerator.producer;


import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Verwaltungsklasse fuer Namensgeneratoren. Liefert zu einer Datei
 * einen entsprechenden Namensgenerator. Wenn moeglich wird eine bestehende
 * Instanz zurueckgegeben.
 */
public class NameProducerManager
{
	private static final Logger LOG = LogManager.getLogger(NameProducerManager.class);

	public static final NameProducerManager INSTANCE = new NameProducerManager();

	private ConcurrentMap<URL, Markov> markovCache = new ConcurrentHashMap<>();
	private ConcurrentMap<URL, ListBasedNameProducer> listBasedCache = new ConcurrentHashMap<>();

	/**
	 * Gibt fuer die angegebene Datei einen Markov-Namensgenerator zurueck.
	 * @param filename Die URL der Datei
	 * @return Der Generator
	 * @throws IllegalStateException Falls die Datei nicht verarbeitet werden kann
	 * @see net.driftingsouls.ds2.server.namegenerator.producer.Markov
	 */
	public NameProducer getMarkovNameProducer(URL filename) throws IllegalStateException
	{
		Markov markov = markovCache.get(filename);
		if (markov != null)
		{
			return markov;
		}

		final MarkovTrainer trainer = new MarkovTrainer();

		LOG.info("Lade Markov-Datei " + filename);

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(filename.openStream())))
		{
			int counter = 0;
			String name;
			while ((name = reader.readLine()) != null)
			{
				name = name.trim();
				if (name.isEmpty())
				{
					continue;
				}
				counter++;
				trainer.add(name);
			}
			LOG.info(counter + " Namen verarbeitet");
		}
		catch (IOException e)
		{
			throw new IllegalStateException("Konnte Markov-Datei '" + filename + "' nicht lesen");
		}

		markovCache.putIfAbsent(filename, trainer.toMarkov());

		return markovCache.get(filename);
	}

	/**
	 * Gibt fuer die angegebene Datei einen listenbasierten Namensgenerator zurueck.
	 * @param filename Die URL der Datei
	 * @param captialize <code>true</code>, falls die Gross-/Kleinschreibung automatisch angepasst werden soll
	 * @return Der Generator
	 * @throws IllegalStateException Falls die Datei nicht verarbeitet werden kann
	 * @see ListBasedNameProducer
	 */
	public NameProducer getListBasedNameProducer(URL filename, boolean captialize) throws IllegalStateException
	{
		ListBasedNameProducer result = listBasedCache.get(filename);
		if (result != null)
		{
			return result;
		}
		listBasedCache.putIfAbsent(filename, new ListBasedNameProducer(filename, captialize));
		return listBasedCache.get(filename);
	}
}
