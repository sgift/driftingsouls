package net.driftingsouls.ds2.server.modules.admin.editoren;

import net.driftingsouls.ds2.server.framework.pipeline.Request;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Klasse zum Erstellen eines Eingabeformulars.
 */
public class EditorForm8<E> implements FormElementCreator<E>
{

	private Function<E,Boolean> allowDelete;
	private final EditorMode modus;
	private Class<?> plugin;
	private List<CustomFieldGenerator<E>> fields = new ArrayList<>();
	private int counter;
	private boolean allowAdd;
	private Function<E,Boolean> allowUpdate;
	private List<Job<E,?>> postAddTasks = new ArrayList<>();
	private List<Job<E,?>> updateTasks = new ArrayList<>();
	private List<Job<E,?>> deleteTasks = new ArrayList<>();
	private Class<? extends E> defaultEntityClass = null;

	public EditorForm8(EditorMode modus, Class<?> plugin)
	{
		this.modus = modus;
		this.counter = 0;
		this.allowAdd = false;
		this.allowDelete = (entity) -> false;
		this.allowUpdate = (entity) -> true;
		this.plugin = plugin;
	}

	/**
	 * Aktiviert das Hinzufuegen von Entities.
	 */
	public void allowAdd()
	{
		this.allowAdd = true;
	}

	/**
	 * Aktiviert das Loeschen von Entities.
	 */
	public void allowDelete()
	{
		this.allowDelete = (entity) -> true;
	}

	/**
	 * Aktiviert das Loeschen von bestimmten Entities
	 */
	public void allowDelete(Function<E,Boolean> condition)
	{
		this.allowDelete = condition;
	}

	/**
	 * Setzt unter welchen Bedingungen das Aktualisieren von Entities moeglich ist.
	 */
	public void allowUpdate(Function<E,Boolean> allowUpdateCondition)
	{
		this.allowUpdate = allowUpdateCondition;
	}

	/**
	 * Gibt zurueck, ob das Hinzufuegen von Entities erlaubt ist.
	 * @return <code>true</code> falls dem so ist
	 */
	public boolean isAddAllowed()
	{
		return this.allowAdd;
	}

	/**
	 * Gibt zurueck, ob das Loeschen der angegebenen Entity erlaubt ist.
	 * @param entity Die Entity
	 * @return <code>true</code> falls dem so ist
	 */
	public boolean isDeleteAllowed(E entity)
	{
		return this.allowDelete.apply(entity);
	}

	/**
	 * Gibt zurueck, ob diese Entity aktualisiert (geaendert) werden kann.
	 * @param entity Die Entity
	 * @return <code>true</code> falls dem so ist
	 */
	public boolean isUpdateAllowed(E entity)
	{
		return this.allowUpdate.apply(entity);
	}

	/**
	 * Fuegt eine Arbeitsaufgabe hinzu, die nach der Aktualisierung (Update) durchgefuehrt werden soll.
	 * Die Aufgabe wird in einer eigenen Transaktion durchgefuehrt.
	 * @param name Der Name der Aufgabe
	 * @param job Die Aufgabe
	 */
	public void postUpdateTask(String name, BiConsumer<E, E> job)
	{
		updateTasks.add(Job.forRunnable(name, job));
	}

	/**
	 * Fuegt eine Arbeitsaufgabe hinzu, die nach der Aktualisierung (Update) durchgefuehrt werden soll.
	 * Die Arbeitsaufgabe wird fuer eine Menge von Objekten durchgefuehrt. Fuer jedes Objekt wird die
	 * Arbeitsaufgabe einzeln aufgerufen. Die Aufgabe wird in einer eigenen Transaktion durchgefuehrt.
	 * Bei groesseren Objektmengen kann die Verarbeitung auch automatisch auf mehrere Transaktionen aufgeteilt werden.
	 * @param name Der Name der Aufgabe
	 * @param supplier Der Generator fuer die Menge der abzuarbeitenden Objekte
	 * @param job Die Aufgabe
	 */
	public <T> void postUpdateTask(String name, Function<E, ? extends Collection<T>> supplier, PostUpdateTaskConsumer<E, T> job)
	{
		updateTasks.add(new Job<>(name, supplier, job));
	}

	/**
	 * Fuegt eine Arbeitsaufgabe hinzu, die nach der Hinzufuegen (Add) durchgefuehrt werden soll.
	 * Die Aufgabe wird in einer eigenen Transaktion durchgefuehrt.
	 * @param name Der Name der Aufgabe
	 * @param job Die Aufgabe
	 */
	public void postAddTask(String name, Consumer<E> job)
	{
		postAddTasks.add(Job.forRunnable(name, job));
	}

	/**
	 * Fuegt eine Arbeitsaufgabe hinzu, die nach der Hinzufuegen (Add) durchgefuehrt werden soll.
	 * Die Arbeitsaufgabe wird fuer eine Menge von Objekten durchgefuehrt. Fuer jedes Objekt wird die
	 * Arbeitsaufgabe einzeln aufgerufen. Die Aufgabe wird in einer eigenen Transaktion durchgefuehrt.
	 * Bei groesseren Objektmengen kann die Verarbeitung auch automatisch auf mehrere Transaktionen aufgeteilt werden.
	 * @param name Der Name der Aufgabe
	 * @param supplier Der Generator fuer die Menge der abzuarbeitenden Objekte
	 * @param job Die Aufgabe
	 */
	public <T> void postAddTask(String name, Function<E, ? extends Collection<T>> supplier, PostUpdateTaskConsumer<E, T> job)
	{
		postAddTasks.add(new Job<>(name, supplier, job));
	}

	/**
	 * Fuegt eine Arbeitsaufgabe hinzu, die vor dem Loeschvorgang durchgefuehrt werden soll.
	 * Die Arbeitsaufgabe wird fuer eine Menge von Objekten durchgefuehrt. Fuer jedes Objekt wird die
	 * Arbeitsaufgabe einzeln aufgerufen. Die Aufgabe wird in einer eigenen Transaktion durchgefuehrt.
	 * Bei groesseren Objektmengen kann die Verarbeitung auch automatisch auf mehrere Transaktionen aufgeteilt werden.
	 * @param name Der Name der Aufgabe
	 * @param supplier Der Generator fuer die Menge der abzuarbeitenden Objekte
	 * @param job Die Aufgabe
	 */
	public <T> void preDeleteTask(String name, Function<E, ? extends Collection<T>> supplier, PostUpdateTaskConsumer<E, T> job)
	{
		deleteTasks.add(new Job<>(name, supplier, job));
	}

	/**
	 * Fuegt eine Arbeitsaufgabe hinzu, die vor dem Loeschvorgang durchgefuehrt werden soll.
	 * Die Aufgabe wird in einer eigenen Transaktion durchgefuehrt.
	 * @param name Der Name der Aufgabe
	 * @param job Die Aufgabe
	 */
	public void preDeleteTask(String name, Consumer<E> job)
	{
		deleteTasks.add(Job.forRunnable(name, job));
	}

	protected List<Job<E,?>> getUpdateTasks()
	{
		return this.updateTasks;
	}
	protected List<Job<E,?>> getDeleteTasks()
	{
		return this.deleteTasks;
	}
	protected List<Job<E,?>> getPostAddTasks()
	{
		return this.postAddTasks;
	}

	@Override
	public <T extends CustomFieldGenerator<E>> T custom(T generator)
	{
		fields.add(generator);
		return generator;
	}

	@Override
	public DynamicContentFieldGenerator<E> dynamicContentField(String label, Function<E, String> getter, BiConsumer<E, String> setter)
	{
		return custom(new DynamicContentFieldGenerator<>(plugin, label, generateName(getter), getter, setter));
	}

	@Override
	public <T> LabelGenerator<E,T> label(String label, Function<E, T> getter)
	{
		return custom(new LabelGenerator<>(generateName(getter), label, getter));
	}

	@Override
	public PictureGenerator<E> picture(String label, Function<E, String> getter)
	{
		return custom(new PictureGenerator<>(generateName(getter), label, getter));
	}

	@Override
	public TextAreaGenerator<E> textArea(String label, Function<E, String> getter, BiConsumer<E, String> setter)
	{
		return custom(new TextAreaGenerator<>(label, generateName(getter), getter, setter));
	}

	@Override
	public <T> FieldGenerator<E,T> field(String label, Class<?> viewType, Class<T> dataType, Function<E, T> getter, BiConsumer<E, T> setter)
	{
		return custom(new FieldGenerator<>(label, generateName(getter), viewType, dataType, getter, setter));
	}

	@Override
	public <T> FieldGenerator<E,T> field(String label, Class<T> type, Function<E, T> getter, BiConsumer<E, T> setter)
	{
		return field(label, type, type, getter, setter);
	}

	/**
	 * Erzeugt eine Auswahl fuer die zu verwendende Entity-Klasse. Es darf nur ein solches Eingabeelement pro Form existieren.
	 * @param label Das Anzeigelabel
	 * @param defaultEntityClass Die Standardmaessig zu verwendende Entity-Klasse
	 * @param options Alle weiteren moeglichen Entity-Klassen
	 */
	public EntityClassGenerator<E> entityClass(String label, Class<? extends E> defaultEntityClass, Class<?> ... options)
	{
		if( this.defaultEntityClass != null )
		{
			throw new IllegalStateException("Es wurde bereits eine Standard-Entityklasse gesetzt. Wurden evt. zwei EntityClass-Felder registriert?");
		}
		this.defaultEntityClass = defaultEntityClass;
		return custom(new EntityClassGenerator<>(label, generateName("entityClass"), modus == EditorMode.CREATE, defaultEntityClass, options));
	}

	@Override
	public <T> MultiSelectionGenerator<E,T> multiSelection(String label, Class<T> type, Function<E, Set<T>> getter, BiConsumer<E, Set<T>> setter)
	{
		return custom(new MultiSelectionGenerator<>(label, generateName(getter), type, type, getter, setter));
	}

	@Override
	public <T, V extends Collection<T>> CollectionGenerator<E, T, V> collection(String label, Class<T> type, Function<E, V> getter, BiConsumer<E, V> setter, Consumer<FormElementCreator<T>> subFormGenerator)
	{
		return custom(new CollectionGenerator<>(label, generateName(getter), type, getter, setter, subFormGenerator, modus, plugin));
	}

	@Override
	public <KT, VT, V extends Map<KT, VT>> MapGenerator<E, KT, VT, V> map(String label, Class<KT> keyType, Class<VT> valueType, Function<E, V> getter, BiConsumer<E, V> setter, Consumer<FormElementCreator<MapEntryRef<KT, VT>>> subFormGenerator)
	{
		return custom(new MapGenerator<>(label, generateName(getter), keyType, valueType, getter, setter, subFormGenerator, modus, plugin));
	}

	private String generateName(Object object)
	{
		String suffix = object instanceof String ? (String)object : object.getClass().getSimpleName();
		return "field"+(counter++)+"_"+ suffix.replace('/', '_').replace('$', '_');
	}

	protected void generateForm(StringBuilder echo, E entity)
	{
		try
		{
			for (CustomFieldGenerator<E> field : fields)
			{
				field.generate(echo, entity);
			}

			if (modus == EditorMode.UPDATE && !updateTasks.isEmpty())
			{
				String str = "<ul>"+updateTasks.stream().map((t) -> "<li>"+t.name+"</li>").collect(Collectors.joining())+"</ul>";
				new LabelGenerator<>("", "Bei Aktualisierung", (e) -> str).generate(echo, entity);
			}
			else if (modus == EditorMode.CREATE && !postAddTasks.isEmpty())
			{
				String str = "<ul>"+postAddTasks.stream().map((t) -> "<li>"+t.name+"</li>").collect(Collectors.joining())+"</ul>";
				new LabelGenerator<>("", "Bei Hinzufügen", (e) -> str).generate(echo, entity);
			}

			if (modus == EditorMode.UPDATE && this.allowUpdate.apply(entity))
			{
				echo.append("<tr><td colspan='2'></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
			}
			else if (modus == EditorMode.CREATE)
			{
				echo.append("<tr><td colspan='2'></td><td><input type=\"submit\" name=\"change\" value=\"Hinzufügen\"></td></tr>\n");
			}

			if( modus != EditorMode.CREATE && this.allowDelete.apply(entity) )
			{
				if (modus == EditorMode.UPDATE && !deleteTasks.isEmpty())
				{
					String str = "<ul>" + deleteTasks.stream().map((t) -> "<li>" + t.name + "</li>").collect(Collectors.joining()) + "</ul>";
					new LabelGenerator<>("", "Beim Löschen", (e) -> str).generate(echo, entity);
				}
				echo.append("<tr><td colspan='2'></td><td><input type=\"submit\" name=\"change\" value=\"Löschen\" onclick=\"return confirm('Sicher?')\"></td></tr>\n");
			}
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}
    
    protected void applyRequestValues(Request request, E entity) throws IOException
	{
        for (CustomFieldGenerator<E> field : fields)
        {
            field.applyRequestValues(request, entity);
        }
    }

	@Override
	public EditorForm8<E> ifAdding()
	{
		if( modus == EditorMode.CREATE )
		{
			return this;
		}
		return new EditorForm8<>(EditorMode.CREATE, plugin);
	}

	@Override
	public EditorForm8<E> ifUpdating()
	{
		if( modus == EditorMode.UPDATE )
		{
			return this;
		}
		return new EditorForm8<>(EditorMode.UPDATE, plugin);
	}

	/**
	 * Schraenkt die Anzeige eines Eingabeelements auf die angegebene Entity-Klasse ein.
	 * @param entityClass Die Entity-Klasse
	 * @return Das Objekt zum Erstellen von Formelementen
	 */
	@SuppressWarnings("unchecked")
	public <T extends E> FormElementCreator<T> ifEntityClass(Class<T> entityClass)
	{
		return new ConditionalFormElementCreator<>(plugin, this::generateName, (FormElementCreator<T>) this, entity -> entityClass.isAssignableFrom(entity.getClass()), modus);
	}

	protected List<ColumnDefinition<E>> getColumnDefinitions(boolean forEditing)
	{
		return this.fields.stream().map(cfg -> cfg.getColumnDefinition(forEditing)).filter(Objects::nonNull).collect(Collectors.toList());
	}

	protected List<String> getEntityValues(E entity)
	{
		return this.fields.stream().map((f) -> f.serializedValueOf(entity)).collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	protected Class<? extends E> getEntityClassRequestValue(Request request, E entity) throws IOException
	{
		Optional<CustomFieldGenerator<E>> field = fields.stream().filter((f) -> f instanceof EntityClassGenerator).findFirst();
		if( !field.isPresent() )
		{
			return (Class<? extends E>) entity.getClass();
		}
		EntityClassGenerator<E> customFieldGenerator = (EntityClassGenerator<E>) field.get();
		customFieldGenerator.applyRequestValues(request, entity);
		return customFieldGenerator.getCurrentEntityClass();
	}

	protected Optional<Class<? extends E>> getDefaultEntityClass()
	{
		return Optional.ofNullable(this.defaultEntityClass);
	}
}
