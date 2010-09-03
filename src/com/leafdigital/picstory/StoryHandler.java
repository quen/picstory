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

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.*;

import javax.imageio.*;
import javax.imageio.stream.FileImageOutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles requests for story page.
 */
public class StoryHandler extends RequestHandler
{
	private final static int BUFFER_SIZE = 64 * 1024;

	private File cacheRoot, storyRoot;

	private static enum Size
	{
		W800(800),
		W600(600),
		W400(400),
		W300(300),
		W200(200),
		W100(100);

		int width;
		Size(int width)
		{
			this.width = width;
		}

		public int getMaxWidth()
		{
			return width;
		}

		public int getMaxHeight()
		{
			return (width * 3) / 4;
		}
	}

	/**
	 * @param mainServlet Main servlet
	 * @param cacheRoot Root folder for cache
	 * @param storyRoot Root folder for stories
	 * @throws ServletException Any error constructing standard objects
	 */
	public StoryHandler(MainServlet mainServlet, File cacheRoot, File storyRoot)
		throws ServletException
	{
		super(mainServlet);
		this.cacheRoot = cacheRoot;
		this.storyRoot = storyRoot;
	}

	/**
	 * @param r Resource name
	 * @param storyName Story name
	 * @throws UserException Error processing story
	 * @throws IOException Any I/O error
	 */
	public void get(Request r, String storyName) throws UserException, IOException
	{
		// Get story
		Story story = getMainServlet().getStories().getStory(
			storyName, r.isReload());

		// Output story
		getMainServlet().sendPage(r, "story", story.getTitle(), story.getContent());
	}

	/**
	 * @param r Request
	 * @param storyName Story name
	 * @param picName Pic name
	 * @param hash Hash (short)
	 * @param sizeString Size string
	 * @throws IOException Any error
	 * @throws UserException File not found, etc
	 */
	public void getPic(Request r, String storyName, String picName, String hash,
		String sizeString) throws IOException, UserException
	{
		// Handle if-modified-since (it never is, because of the hash)
		if(r.handleIfModifiedSince())
		{
			return;
		}

		// Get story and pic
		Story story = getMainServlet().getStories().getStory(
			storyName, r.isReload());
		Pic pic = story.getPic(picName);
		if(pic == null)
		{
			throw new UserException(HttpServletResponse.SC_NOT_FOUND,
				"Picture '" + Util.esc(picName) + "' not found");
		}

		// Redirect if hash changed
		if(!pic.getHash().equals(hash))
		{
			r.redirect(picName + "." + pic.getHash() + "." + sizeString + ".jpg");
			return;
		}

		// Check size is valid
		Size size;
		try
		{
			size = Size.valueOf(sizeString.toUpperCase());
		}
		catch(IllegalArgumentException e)
		{
			throw new UserException(HttpServletResponse.SC_NOT_FOUND,
				"Size '" + sizeString + "' not available");
		}

		// OK, all valid, so let's send it
		r.preventExpiry();
		byte[] buffer = new byte[BUFFER_SIZE];
		File picFile = getPicFile(storyName, pic, size);
		OutputStream out = r.outputBinaryHeaders(
			HttpServletResponse.SC_OK, "image/jpeg", (int)picFile.length());
		FileInputStream in = new FileInputStream(picFile);
		while(true)
		{
			int read = in.read(buffer);
			if(read <= 0)
			{
				break;
			}
			out.write(buffer, 0, read);
		}
		in.close();
		out.close();
	}

	private File getPicFile(String storyName, Pic pic, Size size)
		throws InternalException
	{
		// Look for file in cache folder
		File cache = new File(new File(cacheRoot, storyName),
			pic.getFilename() + "." + pic.getHash() + "."
			+ size.toString().toLowerCase() + ".jpg");
		synchronized(pic)
		{
			if(!cache.exists())
			{
				try
				{
					// Original file
					File original = new File(new File(storyRoot, storyName),
						pic.getFilename() + ".jpg");
					BufferedImage image = ImageIO.read(original);

					// Calculate new size
					int restrictWidth1 = image.getWidth(), restrictHeight1 = image.getHeight();
					if(image.getWidth() > size.getMaxWidth())
					{
						restrictHeight1 = (int)Math.round(
							(double)size.getMaxWidth() / (double)image.getWidth() * image.getHeight());
						restrictWidth1 = size.getMaxWidth();
					}
					int restrictWidth2 = image.getWidth(), restrictHeight2 = image.getHeight();
					if(image.getHeight() > size.getMaxHeight())
					{
						restrictWidth2 = (int)Math.round(
							(double)size.getMaxHeight() / (double)image.getHeight() * image.getWidth());
						restrictHeight2 = size.getMaxHeight();
					}
					int newWidth = Math.min(restrictWidth1, restrictWidth2);
					int newHeight = Math.min(restrictHeight1, restrictHeight2);

					// Image needs resizing
					if(newWidth < image.getWidth() || newHeight < image.getHeight())
					{
						Image scaled = image.getScaledInstance(newWidth, newHeight,
							Image.SCALE_AREA_AVERAGING);
						image = new BufferedImage(newWidth, newHeight,
							BufferedImage.TYPE_INT_RGB);
						image.getGraphics().drawImage(scaled, 0, 0, null);
					}

					// Create directory if required
					File parent = cache.getParentFile();
					if(!parent.exists())
					{
						if(!parent.mkdir())
						{
							throw new InternalException("Error creating folder");
						}
					}

					// Write file
					ImageWriter jpegWriter =
						ImageIO.getImageWritersByFormatName("jpeg").next();
					ImageWriteParam param =	jpegWriter.getDefaultWriteParam();
					param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
					param.setCompressionQuality(0.75f);
					FileImageOutputStream output = new FileImageOutputStream(cache);
					jpegWriter.setOutput(output);
					jpegWriter.write(null, new IIOImage(image, null, null), param);
					output.close();
				}
				catch(Exception e)
				{
					throw new InternalException(
						"Error processing file " + cache.getName(), e);
				}
			}
		}
		return cache;
	}
}
