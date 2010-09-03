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

/**
 * Base class for all request handlers.
 */
public abstract class RequestHandler
{
	private MainServlet mainServlet;

	/**
	 * @param mainServlet Main servlet
	 */
	public RequestHandler(MainServlet mainServlet)
	{
		this.mainServlet = mainServlet;
	}

	/**
	 * @return Main servlet
	 */
	public MainServlet getMainServlet()
	{
		return mainServlet;
	}
}
