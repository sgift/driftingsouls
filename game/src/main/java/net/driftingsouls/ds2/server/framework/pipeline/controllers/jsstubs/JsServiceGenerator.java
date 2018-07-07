package net.driftingsouls.ds2.server.framework.pipeline.controllers.jsstubs;

import net.driftingsouls.ds2.server.framework.AnnotationUtils;
import net.driftingsouls.ds2.server.framework.ViewModel;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.UrlParam;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

public class JsServiceGenerator
{

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

			writer.writeLine("(function() {").indent(1);
			writer.writeLine("var angularModule = angular.module('ds.service.ajax', ['ds.service.ds']);");

			SortedSet<Class<?>> ctrlClasses = AnnotationUtils.INSTANCE.findeKlassenMitAnnotation(Module.class);
			for (Class<?> ctrlClass : ctrlClasses)
			{
				writeJsForController(writer, new ControllerAnalyser(ctrlClass));
			}

			writeViewModelDeclarations(writer);
			writer.indent(-1).writeLine("})();");
		}
	}

	public static void writeViewModelDeclarations(JsWriter writer) throws IOException
	{
		Set<String> processedPackages = new HashSet<>();
		Set<Class<?>> processedViewModels = new HashSet<>();

		while( !processedViewModels.equals(detectedViewModels) )
		{
			for (Class<?> detectedViewModel : new HashSet<>(detectedViewModels))
			{
				if( processedViewModels.contains(detectedViewModel) )
				{
					continue;
				}
				String viewModelName = mapViewModelName(detectedViewModel);
				jsDocDeclareNamespace(writer, viewModelName.substring(0, viewModelName.lastIndexOf('.')), processedPackages);
				writer.writeLine("/**");
				writer.writeLine(" * @typedef {object} " + viewModelName);

				for (Field field : detectedViewModel.getFields())
				{
					if (!Modifier.isPublic(field.getModifiers()) || Modifier.isStatic(field.getModifiers()))
					{
						continue;
					}
					writer.writeLine(" * @property {" + mapTypeToJsDoc(field.getType(), field.getGenericType()) + "} " + field.getName());
				}

				writer.writeLine(" **/");

				processedViewModels.add(detectedViewModel);
			}
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

	private static void writeJsForController(JsWriter writer, ControllerAnalyser controllerAnalyser) throws IOException
	{
		List<Method> actionMethods = controllerAnalyser.findActionMethods();
		if( actionMethods.isEmpty() )
		{
			return;
		}

		writer.writeLine("// "+ controllerAnalyser.getFullName());

		for (Method method : actionMethods)
		{
			writer.writeLine("/**");
			writer.writeLine(" * @class");
			writer.writeLine(" **/");
			writer.writeLine("var DsAjaxPromise_"+controllerAnalyser.getStubName()+"_"+method.getName()+" = {").indent(1);
			writer.writeLine("/**");
			writer.writeLine(" * @name success");
			writer.writeLine(" * @function");
			writer.writeLine(" * @param {function(data:"+mapTypeToJsDoc(method.getReturnType(), null)+")} callback");
			writer.writeLine(" * @memberof DsAjaxPromise_"+controllerAnalyser.getStubName()+"_"+method.getName());
			writer.writeLine(" **/");
			writer.writeLine("success : function(callback) {}");
			writer.indent(-1).writeLine("};");
		}


		writer.writeLine("/**");
		writer.writeLine(" * @class {object} "+ controllerAnalyser.getStubName());
		writer.writeLine(" **/");
		writer.writeLine("function "+ controllerAnalyser.getStubName()+"(ds) {").indent(1);
		writer.writeLine("return {").indent(1);
		boolean first = true;

		for (Method method : actionMethods)
		{
			if( !first ) {
				writer.write(",\n");
			}

			first = false;


			if( method.getParameters().length == 0 ) {
				writeParameterlessStub(writer, controllerAnalyser, method);
			}
			else
			{
				writeMethodWithParameters(writer, controllerAnalyser, method);
			}
		}
		writer.indent(-1).writeLine("").writeLine("}");
		writer.indent(-1).writeLine("}");

		writer.writeLine("angularModule = angularModule.factory('" + controllerAnalyser.getStubName() + "', ['ds', " + controllerAnalyser.getStubName() + "]);");
	}

	private static void writeMethodWithParameters(JsWriter writer, ControllerAnalyser controllerAnalyser, Method method) throws IOException
	{
		String stubName = getStubNameNameFromMethod(method);
		String actionName = getActionNameFromMethod(method);
		writeJsDocFor(writer, controllerAnalyser, method);
		writer.doIndent().write("" + stubName + " : function(");
		writer.write(Arrays.stream(method.getParameters()).map(Parameter::getName).collect(Collectors.joining(",")));
		writer.write(") {\n");
		writer.indent(1).writeLine("var options={};");
		writer.writeLine("options.module='" + controllerAnalyser.getModuleName() + "';");
		writer.writeLine("options.action='" + actionName + "';");
		for (Parameter parameter : method.getParameters())
		{
			UrlParam param = parameter.getAnnotation(UrlParam.class);
			String paramName = param != null ? param.name() : parameter.getName();

			if (Map.class.isAssignableFrom(parameter.getType()) && param != null)
			{
				int idx = param.name().indexOf('#');
				String paramFirstPart = idx == 0 ? "" : param.name().substring(0, idx);
				String paramLastPart = idx >= param.name().length() - 1 ? "" : param.name().substring(idx + 1);
				writer.writeLine("angular.forEach(" + parameter.getName() + ", function(value,key) {").indent(1);
				writer.writeLine("options['" + paramFirstPart + "'+key+'" + paramLastPart + "']=value;");
				writer.indent(-1).writeLine("});");
			}
			else if( Boolean.class.isAssignableFrom(parameter.getType()) || Boolean.TYPE == parameter.getType() )
			{
				writer.writeLine("options." + paramName + "=" + parameter.getName() + " ? 1 : 0;");
			}
			else
			{
				writer.writeLine("options." + paramName + "=" + parameter.getName() + ";");
			}

		}
		writer.writeLine("return ds(options);");
		writer.indent(-1).doIndent().write("}");
	}

	private static void writeJsDocFor(JsWriter writer, ControllerAnalyser controllerAnalyser, Method method) throws IOException
	{
		writer.writeLine("/**");
		writer.writeLine(" * @memberof "+controllerAnalyser.getStubName());
		Parameter[] parameters = method.getParameters();
		for (int i = 0; i < parameters.length; i++)
		{
			Parameter parameter = parameters[i];
			Type type = method.getGenericParameterTypes()[i];
			writer.writeLine(" * @param {" + mapTypeToJsDoc(parameter.getType(), type) + "} " + parameter.getName() + " ");
		}
		writer.writeLine(" * @returns {DsAjaxPromise_"+controllerAnalyser.getStubName()+"_"+method.getName()+"}");
		writer.writeLine(" **/");
	}

	private static void writeParameterlessStub(JsWriter writer, ControllerAnalyser controllerAnalyser, Method method) throws IOException
	{
		String stubName = getStubNameNameFromMethod(method);
		String actionName = getActionNameFromMethod(method);

		writeJsDocFor(writer, controllerAnalyser, method);
		writer.writeLine(stubName + " : function() {").indent(1);
		writer.writeLine("var options = {};");
		writer.writeLine("options.module='" + controllerAnalyser.getModuleName() + "';");
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

		Class<?> enclosingType = cls;
		while((enclosingType = enclosingType.getEnclosingClass()) != null)
		{
			if (enclosingType.isAnnotationPresent(ViewModel.class))
			{
				detectedViewModels.add(cls);
				return mapViewModelName(cls);
			}
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
}
