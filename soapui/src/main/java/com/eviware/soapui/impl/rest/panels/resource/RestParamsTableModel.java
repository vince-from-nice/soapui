/*
 *  soapUI, copyright (C) 2004-2012 smartbear.com 
 *
 *  soapUI is free software; you can redistribute it and/or modify it under the 
 *  terms of version 2.1 of the GNU Lesser General Public License as published by 
 *  the Free Software Foundation.
 *
 *  soapUI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *  See the GNU Lesser General Public License for more details at gnu.org.
 */

package com.eviware.soapui.impl.rest.panels.resource;

import com.eviware.soapui.impl.rest.RestRequest;
import com.eviware.soapui.impl.rest.support.RestParamProperty;
import com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder;
import com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder.ParameterStyle;
import com.eviware.soapui.model.testsuite.TestProperty;
import com.eviware.soapui.model.testsuite.TestPropertyListener;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.eviware.soapui.impl.rest.actions.support.NewRestResourceActionBase.ParamLocation;

public class RestParamsTableModel extends AbstractTableModel implements TableModel, TestPropertyListener,
		PropertyChangeListener
{
	public static final int PARAM_LOCATION_COLUMN_INDEX = 3;
	protected RestParamsPropertyHolder params;
	private List<String> paramNameIndex = new ArrayList<String>(  );

	static String[] COLUMN_NAMES = new String[] { "Name", "Default value", "Style", "Level" };
	static Class[] COLUMN_TYPES = new Class[] { String.class, String.class, ParameterStyle.class, ParamLocation.class };

	public RestParamsTableModel( RestParamsPropertyHolder params )
	{
		this.params = params;
		params.addTestPropertyListener( this );
		if(params.getModelItem() instanceof RestRequest) {
			( ( RestRequest )params.getModelItem() ).getResource().getParams().addTestPropertyListener( this );
			( ( RestRequest )params.getModelItem() ).getResource().addPropertyChangeListener( this );
			( ( RestRequest )params.getModelItem() ).getRestMethod().getParams().addTestPropertyListener( this );
			( ( RestRequest )params.getModelItem() ).getRestMethod().addPropertyChangeListener( this );
		}

		buildParamNameIndex( params );
	}

	private void buildParamNameIndex( RestParamsPropertyHolder params )
	{
		paramNameIndex = new ArrayList<String>( Collections.nCopies( params.size(), "" ));//Initialize with empty values
		for (TestProperty property : params.getProperties().values())
		{
			paramNameIndex.set( params.getPropertyIndex( property.getName() ), property.getName() );
		}
	}

	public void release()
	{
		params.removeTestPropertyListener( this );
	}

	public int getColumnCount()
	{
		return 4;
	}

	@Override
	public String getColumnName( int columnIndex )
	{
		if( isColumnIndexOutOfBound( columnIndex ) )
		{
			return null;
		}
		return COLUMN_NAMES[columnIndex];
	}

	@Override
	public Class<?> getColumnClass( int columnIndex )
	{
		if( isColumnIndexOutOfBound( columnIndex ) )
		{
			return null;
		}
		return COLUMN_TYPES[columnIndex];
	}

	private boolean isColumnIndexOutOfBound( int columnIndex )
	{
		return ( columnIndex < 0 ) || ( columnIndex > 3 );
	}

	@Override
	public boolean isCellEditable( int rowIndex, int columnIndex )
	{
		return true;
	}

	public int getRowCount()
	{
		return params.getPropertyCount();
	}

	public ParamLocation getParamLocationAt( int rowIndex )
	{
		return ( ParamLocation )getValueAt( rowIndex, PARAM_LOCATION_COLUMN_INDEX );
	}


	public Object getValueAt( int rowIndex, int columnIndex )
	{
		RestParamProperty prop = params.getProperty( paramNameIndex.get( rowIndex ) );

		switch( columnIndex )
		{
			case 0:
				return prop.getName();
			case 1:
				return prop.getValue();
			case 2:
				return prop.getStyle();
			case 3:
				return prop.getParamLocation();
		}

		return null;
	}

	@Override
	public void setValueAt( Object value, int rowIndex, int columnIndex )
	{
		RestParamProperty prop = params.getProperty( paramNameIndex.get( rowIndex ) );

		switch( columnIndex )
		{
			case 0:
				params.renameProperty( prop.getName(), value.toString() );
				return;
			case 1:
				//if( !prop.getParamLocation().equals( ParamLocation.REQUEST ) )
				//{
					prop.setDefaultValue( value.toString() );
				//}
				prop.setValue( value.toString() );
				return;
			case 2:
				prop.setStyle( ( ParameterStyle )value );
				return;
			case 3:
				prop.setParamLocation(  ( ParamLocation )value );
				return;
		}
	}

	public RestParamProperty getParameterAt( int selectedRow )
	{
		return params.getProperty( paramNameIndex.get( selectedRow ) );
	}

	public void propertyAdded( String name )
	{
		if(!paramNameIndex.contains( name ))
		{
			paramNameIndex.add( name );
		}
		fireTableDataChanged();
	}

	public void propertyRemoved( String name )
	{
		paramNameIndex.remove( name );
		fireTableDataChanged();
	}

	public void propertyRenamed( String oldName, String newName )
	{
		int paramIndex = paramNameIndex.indexOf( oldName );
		if( paramIndex < 0 )
		{
			return;
		}
		paramNameIndex.set( paramIndex, newName );
		fireTableDataChanged();
	}

	public void propertyValueChanged( String name, String oldValue, String newValue )
	{
		fireTableCellUpdated( paramNameIndex.indexOf( name ), 1 );
	}

	public void propertyMoved( String name, int oldIndex, int newIndex )
	{
		fireTableDataChanged();
	}

	public void moveProperty(String name, int oldIndex, int newIndex)
	{
		String valueAtNewindex = paramNameIndex.get( newIndex );
		paramNameIndex.set( newIndex, name );
		paramNameIndex.set( oldIndex, valueAtNewindex );
		propertyMoved( name, oldIndex, newIndex );
	}

	public ParameterStyle[] getParameterStylesForEdit()
	{
		return new ParameterStyle[] { ParameterStyle.QUERY, ParameterStyle.TEMPLATE, ParameterStyle.HEADER,
				ParameterStyle.MATRIX, ParameterStyle.PLAIN };
	}

	public ParamLocation[] getParameterLevels()
	{
		return ParamLocation.values();
	}

	public void setParams( RestParamsPropertyHolder params )
	{
		this.params.removeTestPropertyListener( this );
		this.params = params;
		this.params.addTestPropertyListener( this );

		buildParamNameIndex( params );
		fireTableDataChanged();
	}

	public void removeProperty( String propertyName )
	{
		params.remove( propertyName );
	}

	@Override
	public void propertyChange( PropertyChangeEvent evt )
	{
		fireTableDataChanged();
	}
}
