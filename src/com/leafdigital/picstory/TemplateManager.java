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
import java.util.*;

/**
 * Retrieves templates from disk or (usually) memory cache.
 */
public class TemplateManager
{
	/**
	 * Template name.
	 */
	public enum Name
	{
		/** Generic page template */
		PAGE("page.xhtml"),
		/** Error message template. */
		ERROR("error.xhtml"),
		/** Story page. */
		STORY("story.xhtml"),
		/** Progress indicator (start). */
		PROGRESS_START("progress.start.xhtml"),
		/** Progress indicator (update). */
		PROGRESS_UPDATE("progress.update.xhtml"),
		/** Progress indicator (finish). */
		PROGRESS_FINISH("progress.finish.xhtml"),
		/** Progress indicator (finish with error). */
		PROGRESS_ERROR("progress.error.xhtml"),
		/** Story XSL (not really a template but whatever). */
		STORY_XSL("story.xsl"),
		/** Index XSL (not really a template but whatever). */
		INDEX_XSL("index.xsl");

		private String filename;
		Name(String filename)
		{
			this.filename = filename;
		}

		/**
		 * @return Filename that corresponds to this template
		 */
		public String getFilename()
		{
			return filename;
		}
	}

	private File templateFolder;
	private ResourceHandler resources;
	private Map<Name, Template> templates = new HashMap<Name, Template>();

	/**
	 * @param templateFolder Folder that contains all templates
	 * @param resources Resources folder
	 */
	public TemplateManager(File templateFolder, ResourceHandler resources)
	{
		this.templateFolder = templateFolder;
		this.resources = resources;
	}

	/**
	 * Gets a template from cache or disk.
	 * @param name Template name
	 * @return Template string
	 * @throws NullPointerException If you specify null for any parameter
	 * @throws IOException If it can't load the template for some reason
	 */
	public synchronized Template get(Name name)
		throws NullPointerException, IOException
	{
		if(name == null)
		{
			throw new NullPointerException();
		}

		// Load template from disk if necessary
		if(!templates.containsKey(name))
		{
			File f = new File(templateFolder, name.getFilename());
			String text = Util.loadString(new FileInputStream(f));
			templates.put(name, new Template(text, resources));
		}

		return templates.get(name);
	}

	/**
	 * Reloads all templates.
	 */
	public synchronized void reload()
	{
		templates.clear();
	}
}
