package net.driftingsouls.ds2.server.modules.admin.editoren;

import net.driftingsouls.ds2.server.framework.pipeline.Request;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

class ConditionalFormElementCreator<E> implements FormElementCreator<E>
{
	private static class ConditionalCustomFieldGenerator<E> implements CustomFieldGenerator<E>
	{
		private Function<E,Boolean> condition;
		private CustomFieldGenerator<E> inner;

		private <T extends CustomFieldGenerator<E>> ConditionalCustomFieldGenerator(T inner, Function<E, Boolean> condition)
		{
			this.condition = condition;
			this.inner = inner;
		}

		@Override
		public void generate(StringBuilder echo, E entity) throws IOException
		{
			if( this.condition.apply(entity) )
			{
				this.inner.generate(echo, entity);
			}
		}

		@Override
		public void applyRequestValues(Request request, E entity) throws IOException
		{
			if( this.condition.apply(entity) )
			{
				this.inner.applyRequestValues(request, entity);
			}
		}

		@Override
		public ColumnDefinition<E> getColumnDefinition(boolean forEditing)
		{
			return this.inner.getColumnDefinition(false);
		}

		@Override
		public String serializedValueOf(E entity)
		{
			if( this.condition.apply(entity) )
			{
				return this.inner.serializedValueOf(entity);
			}
			return "";
		}
	}

	private Class<?> plugin;
	private Function<Object, String> idGenerator;
	private FormElementCreator<E> inner;
	private Function<E,Boolean> condition;
	private EditorMode editorMode;

	ConditionalFormElementCreator(Class<?> plugin, Function<Object,String> idGenerator, FormElementCreator<E> inner, Function<E, Boolean> condition, EditorMode editorMode)
	{
		this.plugin = plugin;
		this.idGenerator = idGenerator;
		this.inner = inner;
		this.condition = condition;
		this.editorMode = editorMode;
	}

	@Override
	public <T extends CustomFieldGenerator<E>> T custom(T generator)
	{
		this.inner.custom(new ConditionalCustomFieldGenerator<>(generator, this.condition));
		return generator;
	}

	@Override
	public DynamicContentFieldGenerator<E> dynamicContentField(String label, Function<E, String> getter, BiConsumer<E, String> setter)
	{
		return custom(new DynamicContentFieldGenerator<>(plugin, label, idGenerator.apply(getter), getter, setter));
	}

	@Override
	public <T> LabelGenerator<E, T> label(String label, Function<E, T> getter)
	{
		return custom(new LabelGenerator<>(idGenerator.apply(getter), label, getter));
	}

	@Override
	public PictureGenerator<E> picture(String label, Function<E, String> getter)
	{
		return custom(new PictureGenerator<>(idGenerator.apply(getter), label, getter));
	}

	@Override
	public TextAreaGenerator<E> textArea(String label, Function<E, String> getter, BiConsumer<E, String> setter)
	{
		return custom(new TextAreaGenerator<>(label, idGenerator.apply(getter), getter, setter));
	}

	@Override
	public <T> FieldGenerator<E, T> field(String label, Class<?> viewType, Class<T> dataType, Function<E, T> getter, BiConsumer<E, T> setter)
	{
		return custom(new FieldGenerator<>(label, idGenerator.apply(getter), viewType, dataType, getter, setter));
	}

	@Override
	public <T> FieldGenerator<E, T> field(String label, Class<T> type, Function<E, T> getter, BiConsumer<E, T> setter)
	{
		return field(label, type, type, getter, setter);
	}

	@Override
	public <T> MultiSelectionGenerator<E, T> multiSelection(String label, Class<T> type, Function<E, Set<T>> getter, BiConsumer<E, Set<T>> setter)
	{
		return custom(new MultiSelectionGenerator<>(label, idGenerator.apply(getter), type, type, getter, setter));
	}

	@Override
	public <T, V extends Collection<T>> CollectionGenerator<E, T, V> collection(String label, Class<T> type, Function<E, V> getter, BiConsumer<E, V> setter, Consumer<FormElementCreator<T>> subFormGenerator)
	{
		return custom(new CollectionGenerator<>(label, idGenerator.apply(getter), type, getter, setter, subFormGenerator, editorMode, plugin));
	}

	@Override
	public <KT, VT, V extends Map<KT, VT>> MapGenerator<E, KT, VT, V> map(String label, Class<KT> keyType, Class<VT> valueType, Function<E, V> getter, BiConsumer<E, V> setter, Consumer<FormElementCreator<MapEntryRef<KT, VT>>> subFormGenerator)
	{
		return custom(new MapGenerator<>(label, idGenerator.apply(getter), keyType, valueType, getter, setter, subFormGenerator, editorMode, plugin));
	}

	@Override
	public FormElementCreator<E> ifAdding()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public FormElementCreator<E> ifUpdating()
	{
		throw new UnsupportedOperationException();
	}
}
