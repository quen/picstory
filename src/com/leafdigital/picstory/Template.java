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

import java.util.*;

/**
 * Represents a template file loaded from disk.
 */
public class Template
{
	private String content;
	private ResourceHandler resources;

	/**
	 * @param content Content of template
	 * @param resources Resource handler
	 */
	Template(String content, ResourceHandler resources)
	{
		this.content = content;
		this.resources = resources;
	}

	/**
	 * Gets the string version of this template.
	 * @param pathToRoot Path to root e.g. "../"
	 * @param fields Input fields
	 * @param values Value of corresponding fields
	 * @return Value as string
	 * @throws IllegalArgumentException If there's a missing field (note: you
	 *   are allowed to specify fields that don't exist)
	 * @throws InternalException Unexpected error
	 * @throws IllegalStateException If content text is invalid
	 */
	public String getString(String pathToRoot, String[] fields, String[] values)
		throws IllegalArgumentException, IllegalStateException, InternalException
	{
		// Check parameters
		if(fields.length != values.length)
		{
			throw new IllegalArgumentException("Unbalanced parameters");
		}

		// Build map
		Map<String, String> fieldValues = new HashMap<String, String>();
		for(int i=0; i<fields.length; i++)
		{
			fieldValues.put(fields[i], values[i]);
		}
		return getString(pathToRoot, fieldValues);
	}

	/**
	 * Gets the string version of this template.
	 * @param pathToRoot Path to root e.g. "../"
	 * @param fields Input fields
	 * @return Value as string
	 * @throws IllegalArgumentException If there's a missing field (note: you
	 *   are allowed to specify fields that don't exist)
	 * @throws IllegalStateException If content text is invalid
	 * @throws InternalException Unexpected error
	 */
	public String getString(String pathToRoot, Map<String, String> fields)
		throws IllegalArgumentException, IllegalStateException, InternalException
	{
		int pos = 0;
		StringBuilder out = new StringBuilder();
		while(true)
		{
			int nextMarker = content.indexOf("%%", pos);
			if(nextMarker == -1)
			{
				out.append(content.substring(pos));
				return out.toString();
			}
			out.append(content.substring(pos, nextMarker));
			int pair = content.indexOf("%%", nextMarker + 2);
			if(pair == -1)
			{
				throw new IllegalStateException("Template has mismatched %%");
			}
			pos = pair + 2;

			String marker = content.substring(nextMarker + 2, pair);
			if(marker.startsWith("R:"))
			{
				String file = marker.substring(2);
				int dot = file.lastIndexOf('.');
				if(dot == -1)
				{
					throw new IllegalStateException(
						"Template has invalid resource filename");
				}
				String base = file.substring(0, dot);
				String extension = file.substring(dot);
				String hash = resources.getHash(file);
				if(hash == null)
				{
					throw new IllegalStateException(
						"Template has unknown resource filename");
				}
				out.append(pathToRoot + "R/" + base + "." + hash + extension);
			}
			else
			{
				String replace = fields.get(marker);
				if(replace == null)
				{
					throw new IllegalArgumentException("Missing field: '" + marker + "'");
				}
				out.append(replace);
			}
		}
	}

	/**
	 * Gets the string version assuming there are no fields.
	 * @return Content
	 */
	public String getString()
	{
		return content;
	}
}
