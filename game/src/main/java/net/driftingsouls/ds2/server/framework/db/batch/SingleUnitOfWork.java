package net.driftingsouls.ds2.server.framework.db.batch;

import net.driftingsouls.ds2.server.framework.ContextMap;

import javax.persistence.EntityManager;
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
		this(name, ContextMap.getContext().getEM());
	}

	public SingleUnitOfWork(String name, EntityManager entityManager) {
		super(name, entityManager);
	}

	@Override
	public final void doWork(Void object) throws Exception
	{
		doWork();
	}

	/**
	 * Fuehrt den isolierten Verarbeitunsschritt aus.
	 * Diese Methode ist von entsprechenden Unterklassen zu implementieren.
     */
	public abstract void doWork();

	/**
	 * Fuehrt die angegebene Aufgabe einmalig aus.
	 */
	public void execute()
	{
		List<Void> fakeParamList = new ArrayList<>();
		fakeParamList.add(null);
		executeFor(fakeParamList);
	}

	@Override
	public SingleUnitOfWork setErrorReporter(UnitOfWorkErrorReporter<Void> errorReporter)
	{
		return (SingleUnitOfWork)super.setErrorReporter(errorReporter);
	}
}
