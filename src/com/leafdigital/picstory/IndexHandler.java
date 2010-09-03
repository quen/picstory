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
import java.text.SimpleDateFormat;
import java.util.*;

import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.*;

/**
 * Handler for the index page (list of stories)
 */
public class IndexHandler extends RequestHandler
{
	private final static long CACHE_EXPIRY = 1L * 60L * 1000L;
	private File cacheRoot, storyRoot;
	private XmlProcessors xml;
	private Cache cache;

	/**
	 * Information held about a single folder.
	 */
	private class Folder implements Comparable<Folder>
	{
		private long updated, date;
		private String folder, error, title, thumbnailUrl;
		private Element description;
		private int thumbnailWidth, thumbnailHeight;

		/**
		 * Constructs an indicator that there's an error with this folder.
		 * @param folder Folder name
		 * @param updated Updated date (from file)
		 * @param error Error text (plain text)
		 */
		Folder(String folder, long updated, String error)
		{
			this.folder = folder;
			this.updated = updated;
			this.error = error;
		}

		/**
		 * Constructs with all data.
		 * @param folder Folder name
		 * @param updated Updated date (from file)
		 * @param date Specified date (from file), 0 if unknown
		 * @param title Title of picstory
		 * @param description Description of picstory
		 * @param thumbnailUrl URL of thumbnail picture for picstory
		 * @param thumbnailWidth Width of thumbnail picture
		 * @param thumbnailHeight Height of thumbnail picture
		 */
		Folder(String folder, long updated, long date, String title,
			Element description, String thumbnailUrl,
			int thumbnailWidth, int thumbnailHeight)
		{
			this.folder = folder;
			this.updated = updated;
			this.date = date;
			this.title = title;
			this.description = description;
			this.thumbnailUrl = thumbnailUrl;
			this.thumbnailWidth = thumbnailWidth;
			this.thumbnailHeight = thumbnailHeight;
		}

		/**
		 * Adds this folder to an XML element.
		 * @param root Element to receive folder child.
		 */
		void add(Element root)
		{
			Document d = root.getOwnerDocument();
			Element folderEl = d.createElement("folder");
			root.appendChild(folderEl);

			folderEl.setAttribute("folder", folder);

			SimpleDateFormat format = Story.newDateFormat();
			folderEl.setAttribute("updated", format.format(new Date(updated)));

			if(title != null)
			{
				folderEl.setAttribute("title", title);
			}

			if(description != null)
			{
				folderEl.appendChild(d.importNode(description, true));
			}

			if(date != 0)
			{
				folderEl.setAttribute("date", format.format(new Date(date)));
			}

			if(thumbnailUrl != null)
			{
				folderEl.setAttribute("thumbnailUrl", thumbnailUrl);
				folderEl.setAttribute("thumbnailWidth", thumbnailWidth + "");
				folderEl.setAttribute("thumbnailHeight", thumbnailHeight + "");
			}

			if(error != null)
			{
				Element errorEl = d.createElement("error");
				folderEl.appendChild(errorEl);
				errorEl.appendChild(d.createTextNode(error));
			}
		}

		@Override
		public int hashCode()
		{
			return folder.hashCode();
		}

		@Override
		public boolean equals(Object obj)
		{
			return (obj != null) && (obj instanceof Folder)
				&& (((Folder)obj).folder.equals(folder));
		}

		@Override
		public int compareTo(Folder o)
		{
			// Use date comparison first
			long thisDate = date == 0 ? updated : date;
			long otherDate = o.date == 0 ? o.updated : o.date;

			if(otherDate < thisDate)
			{
				return -1;
			}
			else if(otherDate > thisDate)
			{
				return 1;
			}
			else
			{
				return folder.compareTo(o.folder);
			}
		}
	}

	private class Cache
	{
		private long loadedDate;
		private String xhtml;

		/**
		 * Loads cache.
		 * @param reload True if cache should be reloaded
		 * @param r Request is used to send data if the process will take a while
		 * @throws InternalException Any error
		 * @throws IOException I/O error
		 */
		private Cache(boolean reload, Request r) throws InternalException, IOException
		{
			File file = getCacheFile();
			long cacheDate = file.lastModified();

			try
			{
				boolean justMade = false;
				if(cacheDate == 0 || reload)
				{
					makeCacheFile(r);
					justMade = true;
				}

				// Load cache
				Document cache = xml.parseFile(file);

				// See if it's out of date
				if(!justMade)
				{
					// Check all folders
					File[] folderFiles = getFolderFiles();
					long lastModified = 0;
					for(File folderFile : folderFiles)
					{
						if(!folderFile.isDirectory())
						{
							continue;
						}
						File index = new File(folderFile, "index.xml");
						long modified = index.lastModified();
						lastModified = Math.max(lastModified, modified);
						if(lastModified > cacheDate)
						{
							break;
						}
					}

					// If necessary, re-make file
					if(lastModified > cacheDate)
					{
						makeCacheFile(r);
						cache = xml.parseFile(file);
					}
				}

				String xsl = getMainServlet().getTemplates().get(
					TemplateManager.Name.INDEX_XSL).getString();
				Document xslDocument = xml.parseString(
					TemplateManager.Name.INDEX_XSL.getFilename(),	xsl);
				xhtml = xml.transform(xslDocument, cache).replace("%%SITENAME%%",
					Util.esc(getMainServlet().getSiteName())).replace("%%INDEXINTRO%%",
					getMainServlet().getIndexIntroXhtml()).replace("%%INDEXFINAL%%",
					getMainServlet().getIndexFinalXhtml());
				loadedDate = System.currentTimeMillis();
			}
			catch(InternalException e)
			{
				xhtml = e.getErrorXhtml(r, getMainServlet());
				loadedDate = System.currentTimeMillis();
			}
		}

		private File getCacheFile()
		{
			return new File(cacheRoot, "index.cache");
		}

		private void makeCacheFile(Request r) throws InternalException, IOException
		{
			// Set up request
			PrintWriter writer = startProgress(r);

			try
			{
				// Prepare XML document
				Document cache = xml.newDocument();
				Element root = cache.createElement("index");
				cache.appendChild(root);

				// Get all folders
				File[] folderFiles = getFolderFiles();

				// Analyse all index.xml files
				Collection<Folder> folderSet = new TreeSet<Folder>();
				for(File folderFile : folderFiles)
				{
					if(!folderFile.isDirectory())
					{
						continue;
					}
					File index = new File(folderFile, "index.xml");
					if(!index.exists())
					{
						continue;
					}

					Folder result;
					String storyName = folderFile.getName();
					try
					{
						updateProgress(writer, storyName);

						// Load story
						Story story = getMainServlet().getStories().getStory(
							storyName, false);

						// Get picture details
						Pic indexPic = story.getIndexPic();
						String picUrl = storyName + "/" + indexPic.getFilename()
							+ "." + indexPic.getHash() + ".w100.jpg";

						// Create folder object
						result = new Folder(storyName, story.getLastModified(),
							story.getDate(), story.getTitle(), story.getDescription(),
							picUrl,	indexPic.getWidth(), indexPic.getHeight());
					}
					catch(Exception e)
					{
						// Create folder object with error message
						result = new Folder(storyName, index.lastModified(),
							e.getMessage());
					}
					folderSet.add(result);
				}

				// Add all folders to cache xml file
				for(Folder folder : folderSet)
				{
					folder.add(root);
				}

				// Save cache file
				FileOutputStream output = new FileOutputStream(getCacheFile());
				output.write(xml.saveString(cache).getBytes(Charset.forName("UTF-8")));
				output.close();

				finishProgress(writer, null);
			}
			catch(Throwable t)
			{
				finishProgress(writer, t);
			}
		}

		/**
		 * @return All folders within storyRoot (empty array if none)
		 */
		private File[] getFolderFiles()
		{
			File[] folderFiles = storyRoot.listFiles();
			if(folderFiles == null)
			{
				folderFiles = new File[0];
			}
			return folderFiles;
		}

		/**
		 * @return Date at which cache was last checked
		 */
		public long getLoadedDate()
		{
			return loadedDate;
		}

		/**
		 * @return XHTML content of page
		 */
		public String getXhtml()
		{
			return xhtml;
		}
	}

	/**
	 * @param mainServlet Main servlet
	 * @param cacheRoot Root folder for cache
	 * @param storyRoot Root folder for stories
	 * @throws InternalException Any error constructing standard objects
	 */
	public IndexHandler(MainServlet mainServlet, File cacheRoot, File storyRoot)
		throws InternalException
	{
		super(mainServlet);
		this.cacheRoot = cacheRoot;
		this.storyRoot = storyRoot;
		xml = new XmlProcessors();
	}

	/**
	 * @param r HTTP request
	 * @throws UserException User error or internal error
	 * @throws IOException Any I/O error
	 */
	public void get(Request r) throws UserException, IOException
	{
		long now = System.currentTimeMillis();
		synchronized(this)
		{
			if(cache == null || cache.getLoadedDate() + CACHE_EXPIRY < now
				|| r.isReload())
			{
				cache = new Cache(r.isReload(), r);
				if(r.sentData())
				{
					return;
				}
			}
		}

		getMainServlet().sendPage(r, "index", null, cache.getXhtml());
	}

	private PrintWriter startProgress(Request r)
		throws IOException, InternalException
	{
		PrintWriter writer = r.outputXhtmlHeaders(HttpServletResponse.SC_OK);
		Template template = getMainServlet().getTemplates().get(
			TemplateManager.Name.PROGRESS_START);
		writer.print(
			template.getString("",
				new String[] {	"SITENAME" },
				new String[] { Util.esc(getMainServlet().getSiteName()) }));
		return writer;
	}

	private void updateProgress(PrintWriter writer, String storyName)
		throws IOException, InternalException
	{
		Template template = getMainServlet().getTemplates().get(
			TemplateManager.Name.PROGRESS_UPDATE);
		writer.println(
			template.getString("",
				new String[] {	"STORYNAME" },
				new String[] { Util.esc(storyName) }));
		for(int i=0; i<20; i++)
		{
			writer.print("          ");
		}
		writer.println();
		writer.flush();
	}

	private void finishProgress(PrintWriter writer, Throwable t)
		throws IOException, InternalException
	{
		if(t == null)
		{
			Template template = getMainServlet().getTemplates().get(
				TemplateManager.Name.PROGRESS_FINISH);
			writer.print(template.getString());
		}
		else
		{
			Template template = getMainServlet().getTemplates().get(
				TemplateManager.Name.PROGRESS_ERROR);
			writer.print(
				template.getString("",
					new String[] { "ERROR", "TRACE" },
					new String[] { Util.esc(t.getMessage()),
					Util.esc(UserException.getTrace(t)) }));
		}
		writer.close();
	}
}
