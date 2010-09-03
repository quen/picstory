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

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.text.*;
import java.util.*;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.sanselan.*;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.tiff.*;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;
import org.w3c.dom.*;

/**
 * A single story.
 */
class Story
{
	private final static Pattern REGEX_PIC = Pattern.compile(
		MainServlet.REGEX_PART_NAME);

	private long lastUsed;

	private long lastModified, date;
	private String title, content;
	private Element description;
	private Pic indexPic;

	private Map<String, Pic> pics = new HashMap<String, Pic>();

	/**
	 * Loads a story from cache file or by creating it afresh (slow).
	 * <p>
	 * NOTE: This method is synchronized inside the story cache.
	 * @param mainServlet Main servlet
	 * @param xml XML processors
	 * @param cacheRoot Cache root folder
	 * @param storyRoot Story root folder
	 * @param storyName Story name
	 * @param lastModified Last modified date of index.xml
	 * @param reload True to reload
	 * @throws InternalException Any processing error
	 * @throws IOException Any I/O error
	 */
	public Story(MainServlet mainServlet, XmlProcessors xml,
		File cacheRoot, File storyRoot,
		String storyName, long lastModified, boolean reload)
		throws InternalException, IOException
	{
		// Check cache folder to see if we already have a cached version of this
		// story
		File cachedStory = new File(new File(cacheRoot, storyName), "story.cache");
		if(!reload)
		{
			long fileLastModified = cachedStory.lastModified();
			// If cache exists and is newer or equal to last modified date of original
			if(fileLastModified >= lastModified)
			{
				Document cache = xml.parseFile(cachedStory);
				Element root = cache.getDocumentElement();
				this.title = root.getElementsByTagName("title").item(0).
					getFirstChild().getNodeValue();
				this.description = (Element)root.getElementsByTagName(
					"description").item(0);
				this.content = root.getElementsByTagName("content").item(0).
					getFirstChild().getNodeValue();
				this.date = Long.parseLong(root.getAttribute("date"));
				NodeList picList = root.getElementsByTagName("pic");
				for(int i=0; i<picList.getLength(); i++)
				{
					Element picEl = (Element)picList.item(i);
					Pic pic = new Pic(picEl);
					pics.put(pic.getFilename(), pic);
					if(pic.isIndexPic())
					{
						indexPic = pic;
					}
				}
				this.lastModified = Long.parseLong(root.getAttribute("lastModified"));
				return;
			}
		}

		this.lastModified = lastModified;

		// Load and parse index file
		File storyFolder = new File(storyRoot, storyName);
		File storyIndex = new File(storyFolder, "index.xml");
		Document d = xml.parseFile(storyIndex);

		// Get date (optional)
		Element rootEl = d.getDocumentElement();
		if(rootEl.hasAttribute("date"))
		{
			try
			{
				date = newDateFormat().parse(rootEl.getAttribute("date")).getTime();
			}
			catch(ParseException e)
			{
				throw new InternalException("Date invalid in '" + storyIndex + "'");
			}
		}

		// Find title
		NodeList titleNodes = d.getElementsByTagName("title");
		if(titleNodes.getLength() != 1)
		{
			throw new InternalException(
				"Unable to find title in '" + storyIndex + "'");
		}
		Node titleTextNode = titleNodes.item(0).getFirstChild();
		if(titleTextNode.getNodeType() != Node.TEXT_NODE)
		{
			throw new InternalException(
				"Unable to obtain title text in '" + storyIndex + "'");
		}
		title = titleTextNode.getNodeValue();

		// Find description
		NodeList descriptionNodes = d.getElementsByTagName("description");
		if(descriptionNodes.getLength() != 1)
		{
			throw new InternalException(
				"Unable to find description in '" + storyIndex + "'");
		}
		description = (Element)descriptionNodes.item(0);

		// Get list of images and process each one
		Collection<Pic> picList = new LinkedList<Pic>();
		NodeList picNodes = d.getElementsByTagName("pic");
		for(int i=0; i<picNodes.getLength(); i++)
		{
			Element picEl = (Element)picNodes.item(i);

			// Check and find picture file
			String picFileName = picEl.getAttribute("src");
			if(!REGEX_PIC.matcher(picFileName).matches())
			{
				throw new InternalException(
					"Picture '" + picFileName + "': not found (invalid name)");
			}
			File picFile = new File(storyFolder, picFileName + ".jpg");
			if(!picFile.exists())
			{
				throw new InternalException(
					"Picture '" + picFileName + "': not found");
			}

			// Give it a numeric id
			picEl.setAttribute("id", "pic" + i);

			// Load image bytes to make hash
			byte[] imageBytes = Util.loadBytes(new FileInputStream(picFile));
			String hash;
			try
			{
				hash = Util.hash(imageBytes).substring(0, 8);
			}
			catch(NoSuchAlgorithmException e)
			{
				throw new InternalException(e);
			}
			picEl.setAttribute("hash", hash);
			picEl.setAttribute("size", "" + imageBytes.length);

			// Load image to get basic data
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
			picEl.setAttribute("width", "" + image.getWidth());
			picEl.setAttribute("height", "" + image.getHeight());

			Pic pic = new Pic(picFileName, hash, imageBytes.length,
				image.getWidth(), image.getHeight());
			if(pic.isIndexPic())
			{
				indexPic = pic;
			}
			picList.add(pic);

			image = null;
			imageBytes = null;

			// Load metadata (this reads the file again, ah well)
			try
			{
				JpegImageMetadata metadata =
					(JpegImageMetadata)Sanselan.getMetadata(picFile);
				if(metadata != null)
				{
					// Set metadata attributes
					TiffField date = metadata.findEXIFValue(
						TiffConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
					if(date != null)
					{
						// wtf - last character sometimes is null
						String dateTimeString = date.getStringValue().replace("\u0000", "");
						if(dateTimeString.length() == 19)
						{
							picEl.setAttribute("date", dateTimeString.substring(0, 10).replace(':', '-'));
							picEl.setAttribute("time", dateTimeString.substring(11));
						}
					}
					TiffField aperture = metadata.findEXIFValue(
						TiffConstants.EXIF_TAG_APERTURE_VALUE);
					if(aperture != null)
					{
						String fStop = "f" + (Math.round(
							Math.pow(Math.sqrt(2), aperture.getDoubleValue()) * 10.0) / 10.0);
						if(fStop.endsWith(".0"))
						{
							fStop = fStop.substring(0, fStop.length() - 2);
						}
						picEl.setAttribute("aperture", fStop);
					}
					TiffField exposureTime = metadata.findEXIFValue(
						TiffConstants.EXIF_TAG_EXPOSURE_TIME);
					if(exposureTime != null)
					{
						double inverse = 1.0 / exposureTime.getDoubleValue();
						String speed;
						if(Math.abs(inverse - Math.round(inverse)) < 0.001)
						{
							speed = "1/" + (int)inverse;
						}
						else
						{
							speed = exposureTime + "s";
						}
						picEl.setAttribute("shutterSpeed", speed);
					}
					TiffField focalLength = metadata.findEXIFValue(
						TiffConstants.EXIF_TAG_FOCAL_LENGTH);
					if(focalLength != null)
					{
						picEl.setAttribute("focalLength", Math.round(focalLength.getDoubleValue()) + "mm");
					}
					TiffField iso = metadata.findEXIFValue(
						TiffConstants.EXIF_TAG_ISO);
					if(iso != null)
					{
						picEl.setAttribute("iso", iso.getIntValue() + "");
					}

					// GPS
					TiffImageMetadata exifMetadata = metadata.getExif();
					if(exifMetadata != null)
					{
						TiffImageMetadata.GPSInfo gpsInfo = exifMetadata.getGPS();
						if(gpsInfo != null)
						{
							double longitude = gpsInfo.getLongitudeAsDegreesEast();
							longitude = Math.round(longitude * 10000000000.0) / 10000000000.0;
							picEl.setAttribute("longitude", "" + longitude);
							double latitude = gpsInfo.getLatitudeAsDegreesNorth();
							latitude = Math.round(latitude * 10000000000.0) / 10000000000.0;
							picEl.setAttribute("latitude", "" + latitude);
							picEl.setAttribute("locationDisplay",
								getPositionString(latitude, "N", "S") + " "
								+ getPositionString(longitude, "E", "W"));
						}
					}
				}
			}
			catch(ImageReadException e)
			{
				throw new InternalException("Picture '" + picFileName
					+ "': error reading", e);
			}
		}

		// Set index pic to first one if none was specified
		if(indexPic == null)
		{
			Pic pic = picList.iterator().next();
			pic.markIndexPic();
			indexPic = pic;
		}

		// Build picture map
		for(Pic pic : picList)
		{
			pics.put(pic.getFilename(), pic);
		}

		// Get XSL via the template mechanism
		String xsl = mainServlet.getTemplates().get(
			TemplateManager.Name.STORY_XSL).getString();
		Document xslDocument = xml.parseString(
			TemplateManager.Name.STORY_XSL.getFilename(),	xsl);
		content = xml.transform(xslDocument, d).replace("%%STORYFINAL%%",
			mainServlet.getStoryFinalXhtml());

		// Get cache file
		Document cache = xml.newDocument();
		rootEl = cache.createElement("cache");
		cache.appendChild(rootEl);
		rootEl.setAttribute("date", date + "");
		rootEl.setAttribute("lastModified", lastModified + "");

		// Version not used now, intended if necessary later if cache format changes
		rootEl.setAttribute("cacheVersion", "1");

		Element titleEl = cache.createElement("title");
		rootEl.appendChild(titleEl);
		titleEl.appendChild(cache.createTextNode(title));
		rootEl.appendChild(cache.importNode(description, true));
		Element contentEl = cache.createElement("content");
		rootEl.appendChild(contentEl);
		contentEl.appendChild(cache.createTextNode(content));
		for(Pic pic : picList)
		{
			pic.add(rootEl);
		}
		String cacheString = xml.saveString(cache);

		// Save it
		if(!cachedStory.getParentFile().exists())
		{
			if(!cachedStory.getParentFile().mkdir())
			{
				throw new InternalException("Unable to create cache folder '"
					+ cachedStory.getParentFile() + "'");
			}
		}
		FileOutputStream out = new FileOutputStream(cachedStory);
		out.write(cacheString.getBytes(Charset.forName("UTF-8")));
		out.close();
	}

	private static String getPositionString(
		double position, String positive, String negative)
	{
		String letter = position >= 0 ? positive : negative;
		double result = Math.abs(position);
		int degrees = (int)Math.floor(result);
		result -= degrees;
		result *= 60;
		int minutes = (int)Math.floor(result);
		result -= minutes;
		result *= 60;
		int seconds = (int)Math.floor(result);
		return degrees + "\u00b0" + minutes + "\u2032"
			+ seconds + "\u2033" + letter;
	}

	/**
	 * Marks this story as accessed so it won't expire from memory cache
	 */
	public void used()
	{
		lastUsed = System.currentTimeMillis();
	}

	/**
	 * @return Last time this story was accessed from cache
	 */
	public long getLastUsed()
	{
		return lastUsed;
	}

	/**
	 * @return Last modified date
	 */
	public long getLastModified()
	{
		return lastModified;
	}

	/**
	 * @return Date of story, or 0 if unspecified
	 */
	public long getDate()
	{
		return date;
	}

	/**
	 * @return Story title
	 */
	public String getTitle()
	{
		return title;
	}

	/**
	 * @return Story description
	 */
	public Element getDescription()
	{
		return description;
	}

	/**
	 * @return Story content
	 */
	public String getContent()
	{
		return content;
	}

	/**
	 * @return Index pic for this file
	 */
	public Pic getIndexPic()
	{
		return indexPic;
	}

	/**
	 * @param filename Filename of picture
	 * @return Pic object or null if none
	 */
	public Pic getPic(String filename)
	{
		return pics.get(filename);
	}

	/**
	 * @return Date format used in various places
	 */
	public static SimpleDateFormat newDateFormat()
	{
		return new SimpleDateFormat("yyyy-MM-dd");
	}
}