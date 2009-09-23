/*
 *  soapUI, copyright (C) 2004-2009 eviware.com 
 *
 *  soapUI is free software; you can redistribute it and/or modify it under the 
 *  terms of version 2.1 of the GNU Lesser General Public License as published by 
 *  the Free Software Foundation.
 *
 *  soapUI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *  See the GNU Lesser General Public License for more details at gnu.org.
 */
package com.eviware.soapui.impl.wsdl.actions.project;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.model.propertyexpansion.PropertyExpander;
import com.eviware.soapui.settings.ToolsSettings;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.action.support.AbstractSoapUIAction;
import com.eviware.soapui.support.components.DirectoryFormComponent;
import com.eviware.soapui.support.components.SimpleForm;

public class StartHermesJMS extends AbstractSoapUIAction<WsdlProject>
{
	public static final String SOAPUI_ACTION_ID = "StarHermesJMS";

	public StartHermesJMS()
	{
		super("Start HermesJMS", "Start HermesJMS application");
	}

	public void perform(WsdlProject project, Object param)
	{
		String hermesConfigPath = chooseFolderDialog(project);
		
		if (hermesConfigPath == null)
			return;

		project.setHermesConfig(hermesConfigPath);
		
		String hermesHome = SoapUI.getSettings().getString(ToolsSettings.HERMES_1_13, "");
		if ("".equals(hermesHome))
		{
			UISupport.showErrorMessage("Please set Hermes 1.13 path in Preferences->Tools ! ");
			return;
		}
		
		startHermesJMS(hermesConfigPath, hermesHome);
	}

	private void startHermesJMS(String hermesConfigPath, String hermesHome)
	{
		String extension = UISupport.isWindows() ? ".bat" : ".sh";
		String hermesBatPath = hermesHome + File.separator + "bin" + File.separator + "hermes" + extension;
		try
		{
			ProcessBuilder pb = new ProcessBuilder(hermesBatPath);
			Map<String, String> env = pb.environment();
			env.put("HERMES_CONFIG", hermesConfigPath);
			pb.start();
		}
		catch (IOException e)
		{
			SoapUI.logError(e);
		}
	}

	private String chooseFolderDialog(WsdlProject project)
	{
		HermesConfigDialog chooseHermesConfigPath = new HermesConfigDialog(PropertyExpander.expandProperties(project, project.getHermesConfig()));
				chooseHermesConfigPath.setVisible(true);
		String hermesConfigPath = chooseHermesConfigPath.getPath();
		return hermesConfigPath;
	}

	private SimpleForm toolsForm;

	public SimpleForm getForm()
	{
		if (toolsForm == null)
		{
			toolsForm = new SimpleForm();
			toolsForm.append("hermes-config.xml path", new DirectoryFormComponent(
					"Location of desired HermesJMS configuration (hermes-config.xml)"));
		}
		return toolsForm;
	}

	private class HermesConfigDialog extends SimpleDialog
	{

		String path;
		DirectoryFormComponent folderComponent;

		public HermesConfigDialog(String initialPath)
		{
			super("Hermes configuration", "Location of desired HermesJMS configuration (hermes-config.xml)", null, true);
			folderComponent.setValue(initialPath);
			folderComponent.setInitialFolder(initialPath);

		}

		protected Component buildContent()
		{

			// name = new JTextField(20);
			// editor = new ReportEditor();
			// editor.setPreferredSize(new Dimension(500, 150));

			SimpleForm form = new SimpleForm();
			folderComponent = new DirectoryFormComponent(
					"Location of desired HermesJMS configuration (hermes-config.xml)");
//			folderComponent.setValue(getPath());
//			String asdf=folderComponent.getValue();
			form.addSpace(5);
			form.append("Path", folderComponent);
			form.addSpace(5);

			return form.getPanel();
		}

		@Override
		protected boolean handleOk()
		{
			setPath(folderComponent.getValue());
			return true;
		}

		public String getPath()
		{
			return path;
		}

		public void setPath(String path)
		{
			this.path = path;
		}

	}
}