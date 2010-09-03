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

import org.w3c.dom.Element;

/**
 * A picture within a story.
 */
class Pic
{
	private String filename, hash;
	private int size, width, height;
	private boolean indexPic;

	Pic(String filename, String hash, int size, int width, int height)
	{
		this.filename = filename;
		this.hash = hash;
		this.size = size;
		this.width = width;
		this.height = height;
	}

	Pic(Element e) throws InternalException
	{
		filename = e.getAttribute("filename");
		hash = e.getAttribute("hash");
		indexPic = "y".equals(e.getAttribute("indexpic"));
		try
		{
			size = Integer.parseInt(e.getAttribute("size"));
			width = Integer.parseInt(e.getAttribute("width"));
			height = Integer.parseInt(e.getAttribute("height"));
		}
		catch(NumberFormatException x)
		{
			throw new InternalException("Cached picture has invalid size/width/height element");
		}
	}

	void add(Element parent)
	{
		Element pic = parent.getOwnerDocument().createElement("pic");
		parent.appendChild(pic);
		pic.setAttribute("filename", filename);
		pic.setAttribute("hash", hash);
		pic.setAttribute("size", size + "");
		pic.setAttribute("width", width + "");
		pic.setAttribute("height", height + "");
		if(indexPic)
		{
			pic.setAttribute("indexpic", "y");
		}
	}

	/**
	 * Marks this picture as the index pic for its story.
	 */
	void markIndexPic()
	{
		indexPic = true;
	}

	/**
	 * @return Filename (not including extension)
	 */
	public String getFilename()
	{
		return filename;
	}

	/**
	 * @return Short hash (8 character)
	 */
	public String getHash()
	{
		return hash;
	}

	/**
	 * @return Size in bytes
	 */
	public int getSize()
	{
		return size;
	}

	/**
	 * @return Width of original image in pixels
	 */
	public int getWidth()
	{
		return width;
	}

	/**
	 * @return Height of original image in pixels
	 */
	public int getHeight()
	{
		return height;
	}

	/**
	 * @return True if this is the index pic
	 */
	public boolean isIndexPic()
	{
		return indexPic;
	}
}