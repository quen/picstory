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

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.*;

/**
 * XML processor objects
 */
public class XmlProcessors
{
	private DocumentBuilder db;
	private TransformerFactory tf;

	/**
	 * @throws InternalException Any error obtaining processors
	 */
	public XmlProcessors() throws InternalException
	{
		try
		{
			DocumentBuilderFactory dbf;
			dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			db = dbf.newDocumentBuilder();
			tf = TransformerFactory.newInstance();
		}
		catch(ParserConfigurationException pce)
		{
			throw new InternalException(pce);
		}
	}

	/**
	 * @param filename Name of file to report in errors
	 * @param document String to parse
	 * @return XML document
	 * @throws InternalException Any error parsing
	 * @throws IOException Probably never
	 */
	public synchronized Document parseString(String filename, String document)
		throws InternalException, IOException
	{
		Document d;
		try
		{
			d = db.parse(new InputSource(new StringReader(document)));
		}
		catch(SAXParseException spe)
		{
			throw new InternalException(
        "XML error in '" + filename + "' on line "
        + spe.getLineNumber() + ", column " + spe.getColumnNumber() + ": "
        + spe.getMessage());
		}
		catch (SAXException se)
		{
			throw new InternalException(
        "XML error in '" + filename + "': "
        + se.getMessage());
		}
		return d;
	}

	/**
	 * @return New empty XML document
	 */
	public synchronized Document newDocument()
	{
		return db.newDocument();
	}

	/**
	 * Parses a file into XML.
	 * @param file File
	 * @return XML document
	 * @throws InternalException Any error parsing
	 * @throws IOException Error reading file
	 */
	public Document parseFile(File file)
		throws InternalException, IOException
	{
		return parseString(file.getName(),
			Util.loadString(new FileInputStream(file)));
	}

	/**
	 * Transforms a document.
	 * @param xslDocument XSL document
	 * @param d Document to transform
	 * @return Resulting document as XHTML string
	 * @throws InternalException Any error parsing
	 * @throws IOException Unlikely error
	 */
	public synchronized String transform(Document xslDocument, Document d)
		throws InternalException, IOException
	{
		StringWriter writer = new StringWriter();
		try
		{
			Transformer t = tf.newTransformer(new DOMSource(xslDocument));
			t.transform(new DOMSource(d), new StreamResult(writer));
		}
		catch(TransformerException e)
		{
			throw new InternalException(
				"Transformation error: " + e.getMessageAndLocation());
		}
		return writer.toString();
	}

	/**
	 * Saves a document to a string.
	 * @param d XML document
	 * @return String for document
	 * @throws InternalException Any error (unlikely)
	 */
	public synchronized String saveString(Document d) throws InternalException
	{
		StringWriter writer = new StringWriter();
		try
		{
			Transformer t = tf.newTransformer();
			t.transform(new DOMSource(d), new StreamResult(writer));
		}
		catch(TransformerException e)
		{
			throw new InternalException(
				"Transformation error: " + e.getMessageAndLocation(), e);
		}
		return writer.toString();
	}

}
