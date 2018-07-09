package net.driftingsouls.ds2.server.framework.pipeline.controllers;

/**
 * Interface fuer Konverter zu einem konkreten Typ aus einem oder mehreren URL-Parametern.
 * @param <T> Der Zieltyp
 */
public interface UrlParamKonverter<T>
{
	/**
	 * Fuehrt den Konvertierungsvorgang durch.
	 * @param parameterReader Der Reader mit dem die URL-Parameter gelesen werden koennen
	 * @param parameterName Der Name des zu konvertierenden Parameters
	 * @return Der konvertiere Parameter
	 */
    T konvertiere(ParameterReader parameterReader, String parameterName);
}
