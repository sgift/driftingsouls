package net.driftingsouls.ds2.server.namegenerator.producer;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Listen basierter Namensgenerator. Quelle ist eine Datei in der pro Zeile genau ein Name
 * steht. Beim generieren wird dann immer ein zufaelliger Name aus der Liste zurueckgegeben.
 */
public class ListBasedNameProducer implements NameProducer
{
	private Random rnd = new Random();
	private List<String> namen = new ArrayList<>();
	private boolean captialize;

	public ListBasedNameProducer(URL filename, boolean capitalize) {
		this.captialize = capitalize;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(filename.openStream(), "UTF-8")))
		{
			String firstName;
			while ((firstName = reader.readLine()) != null)
			{
				namen.add(firstName);
			}
		}
		catch (IOException e)
		{
			throw new IllegalArgumentException("Die angegebene Namensliste '"+filename+"' konnte nicht geladen werden", e);
		}
	}

	@Override
	public String generateNext()
	{
		String name = namen.get(rnd.nextInt(namen.size()));
		return captialize ? StringUtils.capitalize(name.toLowerCase()) : name;
	}
}
