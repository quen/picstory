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

/**
 * Exception in response to user action that contains a status code.
 */
public class UserException extends Exception
{
	private int status;

	/**
	 * @param status HTTP status code (HttpServletResponse.SC_xx)
	 * @param message Message text (for user)
	 */
	public UserException(int status, String message)
	{
		super(message);
		this.status = status;
	}

	/**
	 * @param status HTTP status code (HttpServletResponse.SC_xx)
	 * @param message Message text (for user)
	 * @param cause Exception that caused problem
	 */
	protected UserException(int status, String message, Throwable cause)
	{
		super(message, cause);
		this.status = status;
	}

	/**
	 * Displays this exception to the user.
	 * @param r HTTP request
	 * @param main Main servlet
	 * @throws IOException
	 * @throws NullPointerException
	 * @throws InternalException
	 */
	public void display(Request r, MainServlet main)
		throws NullPointerException, IOException, InternalException
	{
		main.sendPage(r, status, "error", "Error", getErrorXhtml(r, main));
	}

	/**
	 * @param r Request
	 * @param main Main servlet
	 * @return Error page (content only, not outer frame) as XHTML
	 * @throws IOException Any error building page
	 * @throws InternalException Any other error building page
	 */
	public String getErrorXhtml(Request r, MainServlet main) throws IOException,
		InternalException
	{
		// Use templates to build page
		TemplateManager templates = main.getTemplates();
		Template errorTemplate = templates.get(TemplateManager.Name.ERROR);
		String error = errorTemplate.getString(
			r.getPathToRoot(),
			new String[] { "ERROR", "TRACE" },
			new String[] { Util.esc(getMessage()), Util.esc(getTrace(this)) });
		return error;
	}

	/**
	 * @param t Exception to trace
	 * @return Stack trace as string
	 */
	public static String getTrace(Throwable t)
	{
		// Get stack trace
		StringWriter writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		t.printStackTrace(printWriter);
		printWriter.close();
		String trace = writer.toString();
		return trace;
	}
}
