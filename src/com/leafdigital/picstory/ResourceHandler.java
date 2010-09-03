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
import java.security.NoSuchAlgorithmException;
import java.util.*;

import javax.servlet.http.HttpServletResponse;

/**
 * Handles requests for static resources (CSS, etc).
 */
public class ResourceHandler extends RequestHandler
{
	private File resourceFolder;

	private Map<String, Resource> resources = new HashMap<String, Resource>();

	private class Resource
	{
		byte[] data;
		String hash;

		Resource(byte[] data) throws NoSuchAlgorithmException
		{
			this.data = data;
			this.hash = Util.hash(data).substring(0, 8);
		}

		/**
		 * @return Data
		 */
		public byte[] getData()
		{
			return data;
		}

		/**
		 * @return Hash of data
		 */
		public String getShortHash()
		{
			return hash;
		}
	}

	private enum Type
	{
		CSS(".css", "text/css", true),
		JAVASCRIPT(".js", "text/js", true),
		PNG(".png", "image/png", false),
		GIF(".gif", "image/gif", false),
		JPEG(".jpg", "image/jpeg", false);

		private String extension, type;
		private boolean text;
		Type(String extension, String type, boolean text)
		{
			this.extension = extension;
			this.type = type;
			this.text = text;
		}

		/**
		 * @return True if file is a text file
		 */
		public boolean isText()
		{
			return text;
		}

		/**
		 * @return MIME content type
		 */
		public String getType()
		{
			return type;
		}

		/**
		 * @param file Filename
		 * @return Type
		 * @throws InternalException If type unknown
		 */
		public static Type get(String file) throws InternalException
		{
			for(Type type : Type.values())
			{
				if(file.endsWith(type.extension))
				{
					return type;
				}
			}
			throw new InternalException("File type unknown for '" + file + "'");
		}
	}

	/**
	 * @param mainServlet Main servlet
	 * @param resourceFolder Resource folder
	 */
	public ResourceHandler(MainServlet mainServlet, File resourceFolder)
	{
		super(mainServlet);
		this.resourceFolder = resourceFolder;
	}

	/**
	 * @param r HTTP request
	 * @param base Base part of filename
	 * @param hash Expected hash
	 * @param extension Extension part of filename including .
	 * @throws UserException File not found, etc
	 * @throws IOException Error sending data
	 * @throws NoSuchAlgorithmException Problem with Java install
	 */
	public void get(Request r, String base, String hash, String extension)
		throws UserException, IOException, NoSuchAlgorithmException
	{
		// Check for If-Modified-Since (resources use hash so are never modified)
		if(r.handleIfModifiedSince())
		{
			return;
		}
		String file = base + extension;

		// Obtain resource
		Resource resource;
		try
		{
			resource = getResource(file);
		}
		catch(IllegalArgumentException e)
		{
			throw new UserException(HttpServletResponse.SC_NOT_FOUND,
				e.getMessage());
		}

		// Check hash
		if(!resource.getShortHash().equals(hash))
		{
			r.redirect("./" + base + "." + resource.getShortHash() + extension);
			return;
		}

		// Get content type
		Type type = Type.get(file);

		// Don't let this expire
		r.preventExpiry();

		// Send it
		if(type.isText())
		{
			r.outputText(HttpServletResponse.SC_OK, type.getType(),
				new String(resource.getData(), "UTF-8"));
		}
		else
		{
			r.outputBinary(HttpServletResponse.SC_OK,
				type.getType(), resource.getData());
		}
	}

	/**
	 * @param file Filename
	 * @return Resource
	 * @throws IllegalArgumentException File not found etc
	 * @throws NoSuchAlgorithmException Stupid configuration error
	 * @throws IOException Read error
	 */
	private Resource getResource(String file)
		throws IllegalArgumentException, NoSuchAlgorithmException, IOException
	{
		Resource resource;
		synchronized(this)
		{
			if(!resources.containsKey(file))
			{
				// Check filename is safe
				if(!file.matches(MainServlet.REGEX_PART_LCNAME
					+ "\\." + MainServlet.REGEX_PART_LCNAME))
				{
					throw new IllegalArgumentException(
						"Resource '" + Util.esc(file) + "' contains illegal characters");
				}

				// Look for file
				File f = new File(resourceFolder, file);
				if(!f.exists())
				{
					throw new IllegalArgumentException(
						"Resource '" + Util.esc(file) + "' not found");
				}

				// Read file
				resources.put(file,
					new Resource(Util.loadBytes(new FileInputStream(f))));
			}
			resource = resources.get(file);
		}
		return resource;
	}

	/**
	 * Forces the system to reload all resources next time they are accessed.
	 */
	public void reload()
	{
		synchronized(this)
		{
			resources.clear();
		}
	}

	/**
	 * @param file Filename
	 * @return Short hash
	 * @throws IllegalArgumentException File not known
	 * @throws InternalException Unexpected error
	 */
	public String getHash(String file)
		throws IllegalArgumentException, InternalException
	{
		try
		{
			return getResource(file).getShortHash();
		}
		catch(NoSuchAlgorithmException e)
		{
			throw new InternalException(e);
		}
		catch(IOException e)
		{
			throw new InternalException(e);
		}
	}
}
