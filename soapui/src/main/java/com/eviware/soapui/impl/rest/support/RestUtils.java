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

package com.eviware.soapui.impl.rest.support;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.xmlbeans.XmlBoolean;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.rest.RestRequestInterface;
import com.eviware.soapui.impl.rest.support.RestParamsPropertyHolder.ParameterStyle;
import com.eviware.soapui.model.propertyexpansion.DefaultPropertyExpansionContext;
import com.eviware.soapui.model.propertyexpansion.PropertyExpander;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.Tools;
import com.eviware.soapui.support.types.StringList;

public class RestUtils
{
	private final static Pattern splitPattern = Pattern.compile( "[^|]\\|[^|]" );

	public static String[] extractTemplateParams( String path )
	{
		if( StringUtils.isNullOrEmpty( path ) )
			return new String[0];

		StringList result = new StringList();

		int ix = path.indexOf( '{' );
		while( ix != -1 )
		{
			int endIx = path.indexOf( '}', ix );
			if( endIx == -1 )
				break;

			if( endIx > ix + 1 && ( ix > 0 && path.charAt( ix - 1 ) != '$' ) )
				result.add( path.substring( ix + 1, endIx ) );

			ix = path.indexOf( '{', ix + 1 );
		}

		return result.toStringArray();

	}

	public static String extractParams( String pathOrEndpoint, RestParamsPropertyHolder params, boolean keepHost )
	{
		return extractParams( pathOrEndpoint, params, keepHost, true );
	}

	public static String extractParams( String pathOrEndpoint, RestParamsPropertyHolder params, boolean keepHost, boolean createTemplateParams )
	{
		if( StringUtils.isNullOrEmpty( pathOrEndpoint ) )
			return "";

		String path = pathOrEndpoint;
		String queryString = "";
		URL url = null;

		try
		{
			url = new URL( pathOrEndpoint );
			path = url.getPath();
			queryString = url.getQuery();
		}
		catch( MalformedURLException e )
		{
			int ix = path.indexOf( '?' );
			if( ix >= 0 )
			{
				queryString = path.substring( ix + 1 );
				path = path.substring( 0, ix );
			}
		}

		String[] items = path.split( "/" );

		int templateParamCount = 0;
		StringBuffer resultPath = new StringBuffer();

		for( int i = 0; i < items.length; i++ )
		{
			String item = items[i];
			try
			{
				if( item.startsWith( "{" ) && item.endsWith( "}" ) )
				{
					String name = item.substring( 1, item.length() - 1 );
					RestParamProperty property = params.getProperty( name );
					if( !params.hasProperty( name ) )
					{
						property = params.addProperty( name );
					}

					property.setStyle( ParameterStyle.TEMPLATE );
					property.setValue( name );
					property.setDefaultValue( name );
				}
				else
				{
					String[] matrixParams = item.split( ";" );
					if( matrixParams.length > 0 )
					{
						item = matrixParams[0];
						for( int c = 1; c < matrixParams.length; c++ )
						{
							String matrixParam = matrixParams[c];

							int ix = matrixParam.indexOf( '=' );
							if( ix == -1 )
							{
								String name = URLDecoder.decode( matrixParam, "Utf-8" );
								if( !params.hasProperty( name ) )
									params.addProperty( name ).setStyle( ParameterStyle.MATRIX );
							}
							else
							{

								String name = URLDecoder.decode( matrixParam.substring( 0, ix ), "Utf-8" );
								RestParamProperty property = params.getProperty( name );
								if( property == null )
								{
									property = params.addProperty( name );
								}

								property.setStyle( ParameterStyle.MATRIX );
								property.setValue( URLDecoder.decode( matrixParam.substring( ix + 1 ), "Utf-8" ) );
								property.setDefaultValue( URLDecoder.decode( matrixParam.substring( ix + 1 ), "Utf-8" ) );
							}
						}
					}

					if( createTemplateParams )
					{
						Integer.parseInt( item );

						String name = "param" + templateParamCount++ ;
						RestParamProperty property = params.getProperty( name );
						if( !params.hasProperty( name ) )
						{
							property = params.addProperty( name );
						}

						property.setStyle( ParameterStyle.TEMPLATE );
						property.setValue( item );
						property.setDefaultValue( item );

						item = "{" + property.getName() + "}";

					}
				}
			}
			catch( Throwable e )
			{
			}

			if( StringUtils.hasContent( item ) )
				resultPath.append( '/' ).append( item );
		}

		if( StringUtils.hasContent( queryString ) )
		{
			extractParamsFromQueryString( params, queryString );
		}

		if( path.endsWith( "/" ) )
			resultPath.append( '/' );

		if( keepHost && url != null )
		{
			return Tools.getEndpointFromUrl( url ) + resultPath.toString();
		}

		return resultPath.toString();
	}

	public static void extractParamsFromQueryString( RestParamsPropertyHolder params, String queryString )
	{
		String[] items;
		items = queryString.split( "&" );
		for( String item : items )
		{
			try
			{
				int ix = item.indexOf( '=' );
				if( ix == -1 )
				{
					String name = URLDecoder.decode( item, "Utf-8" );

					if( !params.hasProperty( name ) )
					{
						params.addProperty( name ).setStyle( ParameterStyle.QUERY );
					}
				}
				else
				{
					String name = URLDecoder.decode( item.substring( 0, ix ), "Utf-8" );
					RestParamProperty property = params.getProperty( name );
					if( property == null )
					{
						property = params.addProperty( name );
					}

					property.setStyle( ParameterStyle.QUERY );
					property.setValue( URLDecoder.decode( item.substring( ix + 1 ), "Utf-8" ) );
					property.setDefaultValue( URLDecoder.decode( item.substring( ix + 1 ), "Utf-8" ) );
				}
			}
			catch( UnsupportedEncodingException e )
			{
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings( "deprecation" )
	public static String expandPath( String path, RestParamsPropertyHolder params, RestRequestInterface request )
	{
		StringBuffer query = request.isPostQueryString() || "multipart/form-data".equals( request.getMediaType() ) ? null
				: new StringBuffer();
		DefaultPropertyExpansionContext context = new DefaultPropertyExpansionContext( request );

		for( int c = 0; c < params.getPropertyCount(); c++ )
		{
			RestParamProperty param = params.getPropertyAt( c );

			String value = PropertyExpander.expandProperties( context, param.getValue() );
			List<String> valueParts = splitMultipleParameters( value, request.getMultiValueDelimiter() );

			if( value != null && !param.isDisableUrlEncoding() )
			{
				try
				{
					String encoding = System.getProperty( "soapui.request.encoding", request.getEncoding() );

					if( StringUtils.hasContent( encoding ) )
					{
						value = URLEncoder.encode( value, encoding );
						for( int i = 0; i < valueParts.size(); i++ )
							valueParts.set( i, URLEncoder.encode( valueParts.get( i ), encoding ) );
					}
					else
					{
						value = URLEncoder.encode( value );
						for( int i = 0; i < valueParts.size(); i++ )
							valueParts.set( i, URLEncoder.encode( valueParts.get( i ) ) );
					}
				}
				catch( UnsupportedEncodingException e1 )
				{
					SoapUI.logError( e1 );
					value = URLEncoder.encode( value );
					for( int i = 0; i < valueParts.size(); i++ )
						valueParts.set( i, URLEncoder.encode( valueParts.get( i ) ) );
				}
			}

			if( !StringUtils.hasContent( value ) && !param.getRequired() )
				continue;

			if( value == null )
				value = "";

			switch( param.getStyle() )
			{
			case QUERY :
				if( query != null && valueParts != null )
				{
					for( String valuePart : valueParts )
					{
						if( query.length() > 0 )
							query.append( '&' );

						query.append( URLEncoder.encode( param.getName() ) );
						query.append( '=' );

						if( StringUtils.hasContent( valuePart ) )
							query.append( valuePart );
					}
				}
				break;
			case TEMPLATE :
				path = path.replaceAll( "\\{" + param.getName() + "\\}", value );
				break;
			case MATRIX :
				if( param.getType().equals( XmlBoolean.type.getName() ) )
				{
					if( value.toUpperCase().equals( "TRUE" ) || value.equals( "1" ) )
					{
						path += ";" + param.getName();
					}
				}
				else
				{
					path += ";" + param.getName();
					if( StringUtils.hasContent( value ) )
					{
						path += "=" + value;
					}
				}
			case PLAIN :
				break;
			}
		}

		if( query != null && query.length() > 0 )
			path += "?" + query.toString();

		return path;
	}

	// TODO: make it cleaner
	public static String getQueryParamsString( RestParamsPropertyHolder params, RestRequestInterface request )
	{
		StringBuffer query = request.isPostQueryString() || "multipart/form-data".equals( request.getMediaType() ) ? null
				: new StringBuffer();

		for( int c = 0; c < params.getPropertyCount(); c++ )
		{
			RestParamProperty param = params.getPropertyAt( c );

			String value = param.getValue();
			List<String> valueParts = splitMultipleParameters( value, request.getMultiValueDelimiter() );

			if( ( !StringUtils.hasContent( value ) && !param.getRequired() ) || param.getStyle() != ParameterStyle.QUERY )
				continue;

			if( value == null )
				value = "";

			if( query != null && valueParts != null )
			{
				for( String valuePart : valueParts )
				{
					if( query.length() > 0 )
						query.append( '&' );

					query.append( param.getName() );
					query.append( '=' );

					if( StringUtils.hasContent( valuePart ) )
						query.append( valuePart );
				}
			}

		}

		String queryString = "";
		if( query != null && query.length() > 0 )
			queryString += "?" + query.toString();

		return queryString;
	}


	public static List<String> splitMultipleParameters( String paramStr, String delimiter )
	{
		StringList result = new StringList();

		if( StringUtils.hasContent( paramStr ) )
		{
			if( !StringUtils.hasContent( delimiter ) )
			{
				result.add( paramStr );
			}
			else
			{
				result.addAll( paramStr.split( delimiter ) );
			}
		}

		return result;

		// Matcher matcher = splitPattern.matcher( paramStr );
		// List<String> parts = new ArrayList<String>();
		// int i = 0;
		// while( matcher.find() )
		// {
		// parts.add( paramStr.substring( i, matcher.start() + 1 ).replaceAll(
		// "\\|\\|", "|" ) );
		// i = matcher.start() + 2;
		// }
		// parts.add( paramStr.substring( i, paramStr.length() ).replaceAll(
		// "\\|\\|", "|" ) );
		// return parts;
	}

	/**
	 * specificaly used for adding empty parameters also in the list when
	 * "send empty parameters" are checked in HTTP TestRequest Properties
	 * 
	 * @param paramStr
	 * @param delimiter
	 * @return
	 */
	public static List<String> splitMultipleParametersEmptyIncluded( String paramStr, String delimiter )
	{
		StringList result = new StringList();

		if( !StringUtils.hasContent( delimiter ) )
		{
			result.add( paramStr );
		}
		else
		{
			result.addAll( paramStr.split( delimiter ) );
		}

		return result;
	}
}
