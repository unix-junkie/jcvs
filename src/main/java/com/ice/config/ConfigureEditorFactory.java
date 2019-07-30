
package com.ice.config;


@FunctionalInterface
public
interface	ConfigureEditorFactory
	{
	ConfigureEditor
		createEditor(String propertyType);
	}

