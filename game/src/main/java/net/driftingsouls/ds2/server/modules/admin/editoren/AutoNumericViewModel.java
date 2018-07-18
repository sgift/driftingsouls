package net.driftingsouls.ds2.server.modules.admin.editoren;

import net.driftingsouls.ds2.server.framework.ViewModel;

import java.math.BigDecimal;

@ViewModel
public class AutoNumericViewModel
{ public String aSep = "";
	public Number vMin;
	public Number vMax;
	public String lZero = "deny";
	public int mDec;

	/**
	 * Generiert das Model fuer die Javascript-autoNumeric-Bibliothek zu einem bestimmten Datentyp.
	 * Falls der Datentyp nicht fuer autoNumeric unterstuetzt wird, wird <code>null</code> zurueckgegeben.
	 * @param dataType Der Datentyp
	 * @return das Modell oder <code>null</code>
	 */
	public static AutoNumericViewModel forClass(Class<?> dataType)
	{
		if( !Number.class.isAssignableFrom(dataType) )
		{
			return null;
		}

		AutoNumericViewModel model = new AutoNumericViewModel();
		model.mDec = 0;
		model.vMin = -999999999.99;
		model.vMax = 999999999.99;
		if (dataType == Double.class || dataType == Float.class || dataType == BigDecimal.class)
		{
			model.mDec = 8;
		}
		if (dataType == Integer.class)
		{
			model.vMin = Integer.MIN_VALUE;
			model.vMax = Integer.MAX_VALUE;
		}
		else if (dataType == Long.class)
		{
			model.vMin = Long.MIN_VALUE;
			model.vMax = Long.MAX_VALUE;
		}
		else if (dataType == Short.class)
		{
			model.vMin = Short.MIN_VALUE;
			model.vMax = Short.MAX_VALUE;
		}
		else if (dataType == Byte.class)
		{
			model.vMin = Byte.MIN_VALUE;
			model.vMax = Byte.MAX_VALUE;
		}
		return model;
	}
}
