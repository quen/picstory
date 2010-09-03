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

import javax.servlet.http.HttpServletResponse;

/**
 * Exception that indicates a system error occurred.
 */
public class InternalException extends UserException
{
	/**
	 * @param t Cause
	 */
	public InternalException(Throwable t)
	{
		super(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
			"The system was unable to process your request due to an internal error. "
			+ " Please try again later.", t);
	}

	/**
	 * @param message Message
	 */
	public InternalException(String message)
	{
		super(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
	}

	/**
	 * @param message Message
	 * @param t Cause
	 */
	public InternalException(String message, Throwable t)
	{
		super(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message, t);
	}
}
