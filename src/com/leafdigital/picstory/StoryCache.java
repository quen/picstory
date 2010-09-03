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
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

/**
 * Cache of recent stories.
 */
public class StoryCache
{
	private final static Pattern REGEX_STORY = Pattern.compile(
		MainServlet.REGEX_PART_LCNAME);

	/**
	 * Number of stories to cache in RAM (they're still cached on disk otherwise)
	 */
	private final static int STORY_CACHE_SIZE = 10;

	private MainServlet mainServlet;
	private File cacheRoot, storyRoot;
	private Map<String, Story> stories = new HashMap<String, Story>();
	private XmlProcessors xml;

	/**
	 * @param mainServlet Main servlet
	 * @param cacheRoot Root folder for cache
	 * @param storyRoot Root folder for stories
	 * @throws InternalException Any problem creating standard XML stuff
	 */
	public StoryCache(MainServlet mainServlet, File cacheRoot, File storyRoot)
		throws InternalException
	{
		this.mainServlet = mainServlet;
		this.cacheRoot = cacheRoot;
		this.storyRoot = storyRoot;
		xml = new XmlProcessors();
	}

	/**
	 * @param storyName Story name
	 * @param reload If true, reloads story
	 * @return Story
	 * @throws UserException If file not found or some other problem
	 * @throws IOException General error
	 */
	public Story getStory(String storyName, boolean reload)
		throws UserException, IOException
	{
		// Check story exists
		if(!REGEX_STORY.matcher(storyName).matches())
		{
			throw new UserException(HttpServletResponse.SC_NOT_FOUND,
				"Story '" + Util.esc(storyName) + "' not found (illegal characters)");
		}
		File storyFolder = new File(storyRoot, storyName);
		File storyIndex = new File(storyFolder, "index.xml");
		long lastModified = storyIndex.lastModified();
		if(lastModified == 0L)
		{
			throw new UserException(HttpServletResponse.SC_NOT_FOUND,
				"Story '" + Util.esc(storyName) + "' not found");
		}

		// Load story from cache unless it's out of date or not cached or debug mode
		Story story;
		synchronized(this)
		{
			story = stories.get(storyName);
			if(story == null || reload || story.getLastModified() < lastModified)
			{
				story = new Story(mainServlet, xml, cacheRoot, storyRoot, storyName,
					lastModified, reload);
				stories.put(storyName, story);
			}
			story.used();
			while(stories.size() > STORY_CACHE_SIZE)
			{
				long minUsed = Long.MAX_VALUE;
				String remove = null;
				for(Map.Entry<String, Story> entry : stories.entrySet())
				{
					long lastUsed = entry.getValue().getLastUsed();
					if(lastUsed < minUsed)
					{
						minUsed = lastUsed;
						remove = entry.getKey();
					}
				}
				stories.remove(remove);
			}
		}
		return story;
	}
}
