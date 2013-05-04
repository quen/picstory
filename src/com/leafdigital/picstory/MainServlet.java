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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.*;

import javax.servlet.ServletException;
import javax.servlet.http.*;

/**
 * Servlet handles all requests, dispatching them to other classes.
 */
public class MainServlet extends HttpServlet
{
	/**
	 * Hash in URL (8 characters).
	 */
	public final static String REGEX_PART_HASH = "[a-z0-9]{8}";
	/**
	 * Name containing lower-case letters, digits, _ and -
	 */
	public final static String REGEX_PART_LCNAME = "[a-z0-9_-]+";
	/**
	 * Name containing lower-case and upper-case letters, digits, _ and -
	 */
	public final static String REGEX_PART_NAME = "[a-zA-Z0-9_-]+";

	private final static Pattern REGEX_RESOURCE = Pattern.compile(
		"/R/(" + REGEX_PART_LCNAME + ")\\.(" + REGEX_PART_HASH + ")(\\."
		+ REGEX_PART_LCNAME + ")");
	private final static Pattern REGEX_STORY = Pattern.compile(
		"/(" + REGEX_PART_LCNAME + ")/");
	private final static Pattern REGEX_STORY_NO_SLASH = Pattern.compile(
		"/(" + REGEX_PART_LCNAME + ")");
	private final static Pattern REGEX_PIC = Pattern.compile(
		"/(" + REGEX_PART_LCNAME + ")/(" + REGEX_PART_NAME
		+ ")\\.(" + REGEX_PART_HASH + ")\\.(" + REGEX_PART_LCNAME + ")\\.jpg");
	private final static Pattern REGEX_STORY_BASIC_XML = Pattern.compile(
		"/(" + REGEX_PART_LCNAME + ")/basicxml");

	private ResourceHandler resource;
	private StoryHandler story;
	private IndexHandler index;

	private TemplateManager templates;
	private StoryCache stories;
	private String siteName, indexIntroXhtml, indexFinalXhtml, storyFinalXhtml;

	@Override
	public void init() throws ServletException
	{
		System.setProperty("java.awt.headless", "true");
		super.init();
		resource = new ResourceHandler(this, getFolderParameter("resource-folder"));
		File cacheRoot = getFolderParameter("cache-folder");
		File storyRoot = getFolderParameter("story-folder");
		try
		{
			stories = new StoryCache(this, cacheRoot, storyRoot);
			index = new IndexHandler(this, cacheRoot, storyRoot);
		}
		catch(InternalException e)
		{
			throw new ServletException(e);
		}
		story = new StoryHandler(this, cacheRoot, storyRoot);
		templates = new TemplateManager(
			getFolderParameter("template-folder"), resource);
		siteName = getParameter("site-name");
		indexIntroXhtml = getParameter("index-intro");
		indexFinalXhtml = getParameter("index-final");
		storyFinalXhtml = getParameter("story-final");
	}

	private String getParameter(String name) throws ServletException
	{
		String param = getServletConfig().getInitParameter(name);
		if(param == null)
		{
			throw new ServletException("Required parameter '" + name + "' missing");
		}
		return param;
	}

	private File getFolderParameter(String name) throws ServletException
	{
		String param = getParameter(name);

		File result;
		if(param.startsWith("/"))
		{
			result = new File(param);
		}
		else
		{
			result = new File(getServletContext().getRealPath(param));
		}

		if(!result.exists())
		{
			throw new ServletException("Parameter '" + name + "': Path " + result
				+ " not found");
		}

		return result;
	}

	/**
	 * @return Template manager
	 */
	public TemplateManager getTemplates()
	{
		return templates;
	}

	/**
	 * @return Story cache
	 */
	public StoryCache getStories()
	{
		return stories;
	}

	/**
	 * @return Site name
	 */
	public String getSiteName()
	{
		return siteName;
	}

	/**
	 * @return XHTML text that goes at the top of the index page
	 */
	public String getIndexIntroXhtml()
	{
		return indexIntroXhtml;
	}

	/**
	 * @return XHTML text that goes at the bottom of the index page
	 */
	public String getIndexFinalXhtml()
	{
		return indexFinalXhtml;
	}

	/**
	 * @return XHTML text that goes at the bottom of each story page
	 */
	public String getStoryFinalXhtml()
	{
		return storyFinalXhtml;
	}

	/**
	 * Sends a page using the main page template.
	 * @param r Request
	 * @param statusCode Status code (HttpServletResponse.SC_xx)
	 * @param className Class name of page body tag
	 * @param title Title (null = only site title); should not be escaped yet
	 * @param content Main content of page (to be placed inside page template)
	 * @throws IOException Any error
	 * @throws InternalException Unexpected errors
	 */
	public void sendPage(Request r, int statusCode, String className,
		String title, String content)
		throws IOException, InternalException
	{
		if(title == null)
		{
			title = siteName;
		}
		else
		{
			title = siteName + " - " + Util.esc(title);
		}
		Template pageTemplate = templates.get(TemplateManager.Name.PAGE);
		r.outputXhtml(statusCode, pageTemplate.getString(
			r.getPathToRoot(),
			new String[] { "CLASS", "TITLE", "MAIN" },
			new String[] { className, title, content }));
	}

	/**
	 * Sends a page using the main page template, with status 200 OK.
	 * @param r Request
	 * @param className Class name of page body tag
	 * @param title Title (null = only site title)
	 * @param content Main content of page (to be placed inside page template)
	 * @throws IOException Any error
	 * @throws InternalException Unexpected errors
	 */
	public void sendPage(Request r, String className, String title, String content)
		throws IOException, InternalException
	{
		sendPage(r, HttpServletResponse.SC_OK, className, title, content);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
	{
		Request r = new Request(request, response);

		try
		{
			try
			{
				// Refresh templates if required
				if(r.isReload())
				{
					templates.reload();
					resource.reload();
				}

				// Annoyingly, path info is "/" even if you request the page without a /
				// on the end; handle both normal behaviour and this weird behaviour
				String path = request.getPathInfo();
				if(path == null || path.equals("") ||
					(path.equals("/") && !request.getRequestURI().endsWith("/")))
				{
					r.redirect(request.getRequestURI() + "/");
					return;
				}

				if(path.equals("/"))
				{
					index.get(r);
				}

				Matcher m = REGEX_RESOURCE.matcher(path);
				if(m.matches())
				{
					resource.get(r, m.group(1), m.group(2), m.group(3));
					return;
				}

				m = REGEX_STORY_BASIC_XML.matcher(path);
				if(m.matches())
				{
					story.getBasicXml(r, m.group(1));
					return;
				}

				m = REGEX_STORY_NO_SLASH.matcher(path);
				if(m.matches())
				{
					r.redirect(m.group(1) + "/");
					return;
				}

				m = REGEX_STORY.matcher(path);
				if(m.matches())
				{
					story.get(r, m.group(1));
					return;
				}

				m = REGEX_PIC.matcher(path);
				if(m.matches())
				{
					story.getPic(r, m.group(1), m.group(2), m.group(3), m.group(4));
					return;
				}

				throw new UserException(HttpServletResponse.SC_NOT_FOUND,
					"Path '" + path + "' not found");
			}
			catch(UserException e)
			{
				throw e;
			}
			catch(Throwable t)
			{
				throw new InternalException(t);
			}
		}
		catch(UserException e)
		{
			if(e instanceof InternalException)
			{
				System.err.println(
					new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
					+ " ** picstory exception");
				e.printStackTrace();
				System.err.println();
			}
			try
			{
				e.display(r, this);
			}
			catch(InternalException e2)
			{
				throw new ServletException(e2);
			}
		}
	}
}
