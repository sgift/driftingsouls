package net.driftingsouls.ds2.server.modules.admin.editoren;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Ein Job, der bei CRUD-Operationen auf Entities ausgefuehrt werden kann.
 * @param <E> Der Entity-Typ auf dem der Job ausgefuehrt werden soll
 * @param <T> Der fuer jede Jobiteration als Parameter verwendete Datentyp
 */
public class Job<E,T>
{
	public final String name;
	public final Function<E, ? extends Collection<T>> supplier;
	public final PostUpdateTaskConsumer<E, T> job;

	public Job(String name, Function<E, ? extends Collection<T>> supplier, PostUpdateTaskConsumer<E, T> job)
	{
		this.name = name;
		this.supplier = supplier;
		this.job = job;
	}

	public static <E> Job<E, Boolean> forRunnable(String name, BiConsumer<E, E> job)
	{
		return new Job<>(name, (entity) -> Arrays.asList(Boolean.TRUE), (o, e, b) -> job.accept(o, e));
	}

	public static <E> Job<E, Boolean> forRunnable(String name, Consumer<E> job)
	{
		return new Job<>(name, (entity) -> Arrays.asList(Boolean.TRUE), (o, e, b) -> job.accept(e));
	}
}
