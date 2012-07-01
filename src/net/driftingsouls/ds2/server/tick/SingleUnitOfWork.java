package net.driftingsouls.ds2.server.tick;

import java.util.ArrayList;
import java.util.List;

/**
 * Klasse zum Durchfuehren einer einzigen isolierten Aufgabe.
 * Entspricht weitestgehend {@link UnitOfWork}, jedoch mit entsprechenden
 * Anpassungen um eine einzelne Aufgabe besser erzeugen zu koennen.
 * @author christopherjung
 * @see UnitOfWork
 */
public abstract class SingleUnitOfWork extends UnitOfWork<Void>
{

	public SingleUnitOfWork(String name)
	{
		super(name);
	}

	@Override
	public final void doWork(Void object) throws Exception
	{
		doWork();
	}

	/**
	 * Fuehrt den isolierten Verarbeitunsschritt aus.
	 * Diese Methode ist von entsprechenden Unterklassen zu implementieren.
	 * @throws Exception Generelle Verarbeitungsfehler, die zu einem Abbruch der Transaktion fuehren sollen
	 */
	public abstract void doWork() throws Exception;

	/**
	 * Fuehrt die angegebene Aufgabe einmalig aus.
	 */
	public void execute()
	{
		List<Void> fakeParamList = new ArrayList<Void>();
		fakeParamList.add(null);
		executeFor(fakeParamList);
	}
}
