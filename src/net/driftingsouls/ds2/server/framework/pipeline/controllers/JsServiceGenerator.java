package net.driftingsouls.ds2.server.framework.pipeline.controllers;

import net.driftingsouls.ds2.server.framework.AnnotationUtils;
import net.driftingsouls.ds2.server.framework.ViewModel;
import net.driftingsouls.ds2.server.framework.pipeline.Module;

import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

public class JsServiceGenerator
{
	private static class JsWriter implements Closeable
	{
		private Writer writer;
		private int indent;

		private JsWriter(Writer writer)
		{
			this.writer = writer;
		}

		public JsWriter writeLine(String text) throws IOException
		{
			doIndent();
			writer.write(text+"\n");
			return this;
		}

		public JsWriter doIndent() throws IOException
		{
			for (int i = 0; i < indent; i++)
			{
				writer.write('\t');
			}
			return this;
		}

		public JsWriter write(String text) throws IOException
		{
			writer.write(text);
			return this;
		}

		public JsWriter indent(int indentMod)
		{
			indent += indentMod;
			return this;
		}

		@Override
		public void close() throws IOException
		{
			writer.close();
		}
	}

	private static Set<Class<?>> detectedViewModels = new HashSet<>();

	public static void main(String[] args) throws IOException
	{
		if( args.length != 1 ) {
			System.err.println(JsServiceGenerator.class.getSimpleName()+" [Ausgabedatei]");
			return;
		}

		try (JsWriter writer = new JsWriter(new FileWriter(args[0])))
		{
			writer.writeLine("/*").indent(1);
			writer.writeLine("Automatisch generierte Datei. Zum generieren bitte JsServiceGenerator aufrufen.");
			writer.indent(-1).writeLine("*/");
			writer.writeLine("angular.module('ds.ajax', ['ds.service.ds'])");

			SortedSet<Class<?>> ctrlClasses = AnnotationUtils.INSTANCE.findeKlassenMitAnnotation(Module.class);
			for (Class<?> ctrlClass : ctrlClasses)
			{
				writeJsForController(writer, ctrlClass);
			}
			writer.writeLine(";");

			writeViewModelDeclarations(writer);
		}
	}

	public static void writeViewModelDeclarations(JsWriter writer) throws IOException
	{
		Set<String> processedPackages = new HashSet<>();

		for (Class<?> detectedViewModel : detectedViewModels)
		{
			String viewModelName = mapViewModelName(detectedViewModel);
			jsDocDeclareNamespace(writer, viewModelName.substring(0, viewModelName.lastIndexOf('.')), processedPackages);
			writer.writeLine("/**");
			writer.writeLine(" * @typedef {object} "+viewModelName);
			writer.writeLine(" **/");
		}
	}

	private static void jsDocDeclareNamespace(JsWriter writer, String namespace, Set<String> declaredNamespaces) throws IOException
	{
		if( declaredNamespaces.contains(namespace) )
		{
			return;
		}
		if( namespace.indexOf('.') > -1 )
		{
			String parent = namespace.substring(0, namespace.lastIndexOf('.'));
			jsDocDeclareNamespace(writer, parent, declaredNamespaces);
		}
		writer.writeLine("/**");
		writer.writeLine(" * @namespace "+namespace);
		writer.writeLine(" **/");

		declaredNamespaces.add(namespace);
	}

	private static void writeJsForController(JsWriter writer, Class<?> ctrlClass) throws IOException
	{
		Module module = ctrlClass.getAnnotation(Module.class);

		writer.writeLine("// "+ctrlClass.getCanonicalName());
		writer.writeLine(".factory('"+getStubNameFromClass(ctrlClass)+"', ['ds', function(ds) {")
				.indent(1)
				.writeLine("return {")
				.indent(1);

		boolean first = true;

		for (Method method : ctrlClass.getMethods())
		{
			if( !method.isAnnotationPresent(Action.class) )
			{
				continue;
			}
			Action action = method.getAnnotation(Action.class);
			if( action.value() != ActionType.AJAX )
			{
				continue;
			}

			if( !first ) {
				writer.write(",\n");
			}

			first = false;


			if( method.getParameters().length == 0 ) {
				writeParameterlessStub(writer, module, method);
			}
			else
			{
				writeMethodWithParameters(writer, module, method);
			}
		}

		writer.indent(-1)
				.writeLine("")
				.writeLine("}")
				.indent(-1).writeLine("}])");
	}

	private static void writeMethodWithParameters(JsWriter writer, Module module, Method method) throws IOException
	{
		String stubName = getStubNameNameFromMethod(method);
		String actionName = getActionNameFromMethod(method);
		writeJsDocFor(writer, method);
		writer.doIndent().write("" + stubName + " : function(");
		writer.write(Arrays.stream(method.getParameters()).map(Parameter::getName).collect(Collectors.joining(",")));
		writer.write(") {\n");
		writer.indent(1).writeLine("var options={};");
		writer.writeLine("options.module='" + module.name() + "';");
		writer.writeLine("options.action='" + actionName + "';");
		for (Parameter parameter : method.getParameters())
		{
			if( parameter.isAnnotationPresent(UrlParam.class) )
			{
				UrlParam param = parameter.getAnnotation(UrlParam.class);
				if( Map.class.isAssignableFrom(parameter.getType()) )
				{
					int idx = param.name().indexOf('#');
					String paramFirstPart = idx == 0 ? "" : param.name().substring(0, idx);
					String paramLastPart = idx >= param.name().length()-1  ? "" : param.name().substring(idx+1);
					writer.writeLine("angular.forEach("+parameter.getName()+", function(key,value) {").indent(1);
					writer.writeLine("options['" + paramFirstPart + "'+key+'" + paramLastPart + "']=value;");
					writer.indent(-1).writeLine("});");
				}
				else
				{
					writer.writeLine("options." + param.name() + "=" + parameter.getName() + ";");
				}
			}
			else
			{
				writer.writeLine("options." + parameter.getName() + "=" + parameter.getName() + ";");
			}
		}
		writer.writeLine("return ds(options);");
		writer.indent(-1).doIndent().write("}");
	}

	private static void writeJsDocFor(JsWriter writer, Method method) throws IOException
	{
		writer.writeLine("/**");
		writer.writeLine(" *");
		Parameter[] parameters = method.getParameters();
		for (int i = 0; i < parameters.length; i++)
		{
			Parameter parameter = parameters[i];
			Type type = method.getGenericParameterTypes()[i];
			writer.writeLine(" * @param {" + mapTypeToJsDoc(parameter.getType(), type) + "} " + parameter.getName() + " ");
		}
		writer.writeLine(" * @returns {"+mapTypeToJsDoc(method.getReturnType(), null)+"}");
		writer.writeLine(" **/");
	}

	private static void writeParameterlessStub(JsWriter writer, Module module, Method method) throws IOException
	{
		String stubName = getStubNameNameFromMethod(method);
		String actionName = getActionNameFromMethod(method);

		writeJsDocFor(writer, method);
		writer.writeLine(stubName + " : function() {").indent(1);
		writer.writeLine("var options = {};");
		writer.writeLine("options.module='" + module.name() + "';");
		writer.writeLine("options.action='" + actionName + "';");
		writer.writeLine("return ds(options);");
		writer.indent(-1).doIndent().write("}");
	}

	private static String mapTypeToJsDoc(Class cls, Type type)
	{
		if (cls == String.class)
		{
			return "string";
		}
		else if (Number.class.isAssignableFrom(cls) || cls == Integer.TYPE ||
				cls == Long.TYPE || cls == Double.TYPE ||
				cls == Float.TYPE || cls == Byte.TYPE ||
				cls == Short.TYPE )
		{
			return "number";
		}
		else if(Boolean.class == cls || cls == Boolean.TYPE )
		{
			return "boolean";
		}
		else if(Map.class.isAssignableFrom(cls) )
		{
			Type[] arguments = ((ParameterizedType)type).getActualTypeArguments();
			return "object.<"+mapTypeToJsDoc((Class)arguments[0], null)+","+mapTypeToJsDoc((Class)arguments[1], null)+">";
		}
		else if( cls.isAnnotationPresent(ViewModel.class) )
		{
			detectedViewModels.add(cls);
			return mapViewModelName(cls);
		}

		return "object";
	}

	private static String mapViewModelName(Class cls)
	{
		String name = cls.getCanonicalName().substring("net.driftingsouls.ds2.server.".length());
		return "ds.viewmodel."+name;
	}

	private static String getStubNameNameFromMethod(Method method)
	{
		String name = method.getName();

		if( name.endsWith("Action") )
		{
			name = name.substring(0, name.length() - "Action".length());
		}
		else if( name.endsWith("AjaxAct") )
		{
			name = name.substring(0, name.length() - "AjaxAct".length());
		}

		if( "default".equals(name) )
		{
			name = "defaultAction";
		}

		return name;
	}

	private static String getActionNameFromMethod(Method method)
	{
		String name = method.getName();

		if( name.endsWith("Action") )
		{
			name = name.substring(0, name.length() - "Action".length());
		}
		else if( name.endsWith("AjaxAct") )
		{
			name = name.substring(0, name.length() - "AjaxAct".length());
		}

		return name;
	}

	private static String getStubNameFromClass(Class<?> ctrlClass)
	{
		return ctrlClass.getSimpleName()+"Stub";
	}
}
