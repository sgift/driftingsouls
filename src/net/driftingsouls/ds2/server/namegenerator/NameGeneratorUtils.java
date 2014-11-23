package net.driftingsouls.ds2.server.namegenerator;

/**
 * Hilfsfunktionen fuer Namensgeneratoren.
 */
public final class NameGeneratorUtils
{
	private NameGeneratorUtils()
	{
		// EMPTY
	}

	/**
	 * <p>Wandelt den ersten Buchstaben nach allen Vorkommnissen der angegebenen Strings
	 * in einen Grossbuchstaben um.</p>
	 * @param name Der zu durchsuchende String
	 * @param str Die Liste der Strings hinter dem der Buchstabe in einen Grossbuchstaben umzuwandeln sind
	 * @return Der umgewandelte String
	 * @see #upperAfterString(String, String)
	 */
	public static String upperAfterStrings(String name, String... str)
	{
		for (String s : str)
		{
			name = upperAfterString(name, s);
		}
		return name;
	}

	/**
	 * <p>Wandelt den ersten Buchstaben nach allen Vorkommnissen des angegebenen
	 * Strings in einen Grossbuchstaben um.</p>
	 * <p>Beispiel: <code>upperAfterString("mclachlan", "mc") == "mcLachlan"</code></p>
	 * @param name Der zu durchsuchende String
	 * @param str Der String hinter dem der Buchstabe in einen Grossbuchstaben umzuwandeln ist
	 * @return Der umgewandelte String
	 */
	public static String upperAfterString(String name, String str)
	{
		int index = -1;
		while ((index = name.indexOf(str, index + 1)) > -1)
		{
			if (index + 1 + str.length() >= name.length())
			{
				break;
			}
			String firstPart = name.substring(0, index);
			String middlePart = name.substring(index, index + str.length());
			String endPart = name.substring(index + str.length());

			name = firstPart + middlePart + Character.toUpperCase(endPart.charAt(0)) + endPart.substring(1);
		}
		return name;
	}
}
