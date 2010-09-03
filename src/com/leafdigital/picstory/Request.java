/*
This file is part of leafdigital picstory.

picstory is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

picstory is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with picstory.  If not, see <http://www.gnu.org/licenses/>.

Copyright 2010 Samuel Marshall.
*/
package com.leafdigital.picstory;

import java.io.*;
import java.nio.charset.Charset;

import javax.servlet.http.*;

/**
 * Holds details of an HTTP request.
 */
public class Request
{
	/**
	 * Constant indicating that the length of output data is unknown.
	 */
	public final static int UNKNOWN_LENGTH = -1;

	private final static long ONE_YEAR = 365L * 24L * 3600L * 1000L;
	private HttpServletRequest request;
	private HttpServletResponse response;
	private boolean sentData;

	/**
	 * @param request HTTP request
	 * @param response HTTP response
	 */
	public Request(HttpServletRequest request, HttpServletResponse response)
	{
		this.request = request;
		this.response = response;
	}

	/**
	 * @return Servlet request
	 */
	public HttpServletRequest getRequest()
	{
		return request;
	}

	/**
	 * @return Servlet response
	 */
	public HttpServletResponse getResponse()
	{
		return response;
	}

	/**
	 * @return True if this is a debug request (?debug parameter)
	 */
	public boolean isReload()
	{
		return request.getParameter("reload") != null;
	}

	/**
	 * @return True if we have already sent (or started sending) data
	 */
	public boolean sentData()
	{
		return sentData;
	}

	/**
	 * Outputs a page as XHTML if possible.
	 * @param statusCode Status code (HttpServletResponse.SC_xx)
	 * @param page Page content
	 * @throws IOException Any error
	 */
	public void outputXhtml(int statusCode, String page) throws IOException
	{
		outputText(statusCode, getXhtmlMimeType(), page);
	}

	/**
	 * @return MIME type to use for an XHTML response to this request
	 */
	private String getXhtmlMimeType()
	{
		String accept = request.getHeader("accept");
		boolean xhtml = accept != null && accept.contains("application/xhtml+xml");
		return xhtml ? "application/xhtml+xml" : "text/html";
	}

	/**
	 * Outputs a page as text (UTF-8).
	 * @param statusCode Status code
	 * @param contentType Content type
	 * @param page Page content
	 * @throws IOException Any error
	 */
	public void outputText(int statusCode, String contentType, String page)
		throws IOException
	{
		response.setCharacterEncoding("UTF-8");
		outputBinary(statusCode, contentType,
			page.getBytes(Charset.forName("UTF-8")));
	}

	/**
	 * Outputs binary data.
	 * @param statusCode Status code
	 * @param contentType Content type
	 * @param data Data
	 * @throws IOException Any error
	 */
	public void outputBinary(int statusCode, String contentType, byte[] data)
		throws IOException
	{
		sentData = true;
		OutputStream out = outputBinaryHeaders(statusCode, contentType, data.length);
		out.write(data);
		out.close();
	}

	/**
	 * @param statusCode Status code
	 * @return Output writer that you should now write the data to
	 * @throws IOException Any error
	 */
	public PrintWriter outputXhtmlHeaders(int statusCode) throws IOException
	{
		sentData = true;
		response.setContentType(getXhtmlMimeType());
		response.setCharacterEncoding("UTF-8");
		response.setStatus(statusCode);
		return response.getWriter();
	}

	/**
	 * @param statusCode Status code
	 * @param contentType Content type
	 * @param length Length in bytes or UNKNOWN_LENGTH if unknown
	 * @return Output stream that you should now write the data to
	 * @throws IOException Any error
	 */
	public OutputStream outputBinaryHeaders(int statusCode, String contentType,
		int length) throws IOException
	{
		sentData = true;
		response.setContentType(contentType);
		response.setStatus(statusCode);
		if(length != UNKNOWN_LENGTH)
		{
			response.setContentLength(length);
		}
		return response.getOutputStream();
	}

	/**
	 * @param url URL for redirect
	 * @throws IOException Any error
	 */
	public void redirect(String url) throws IOException
	{
		sentData = true;
		response.sendRedirect(url);
	}

	/**
	 * Ensures that any If-Modified-Since header results in a Not Modified
	 * response.
	 * @return True if request includes If-Modified-Since header and a response
	 *   has been sent
	 */
	public boolean handleIfModifiedSince()
	{
		long modified = request.getDateHeader("If-Modified-Since");
		if(modified != -1)
		{
			response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return true;
		}
		return false;
	}

	/**
	 * Make sure result doesn't expire from cache
	 */
	public void preventExpiry()
	{
		response.addDateHeader("Expires", System.currentTimeMillis() + ONE_YEAR);
	}

	/**
	 * @return Path to root e.g. "../"
	 * @throws InternalException If this doesn't work for some reason
	 */
	public String getPathToRoot() throws InternalException
	{
		String requestUri = request.getRequestURI();
		String baseUri = request.getContextPath();
		int start = requestUri.indexOf(baseUri);
		if(start == -1)
		{
			throw new InternalException("Unable to figure out base URL");
		}
		String remainingPath = requestUri.substring(start + baseUri.length());
		int remainingSlashes = remainingPath.replaceAll("[^/]", "").length();
		String pathToRoot = "";
		for(int i=1; i<remainingSlashes; i++)
		{
			pathToRoot += "../";
		}
		return pathToRoot;
	}
}
