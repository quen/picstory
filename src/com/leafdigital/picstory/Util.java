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
import java.math.BigInteger;
import java.security.*;

/**
 * Utility methods.
 */
public abstract class Util
{
	private final static int BUFFERSIZE=65536;

	/**
	 * Loads all the bytes from an inputstream, then closes it.
	 * @param is Stream
	 * @return Bytes
	 * @throws IOException
	 */
	public static byte[] loadBytes(InputStream is) throws IOException
	{
		byte[] buffer=new byte[BUFFERSIZE];
		int pos=0;
		while(true)
		{
			// Read data
			int read=is.read(buffer,pos,buffer.length-pos);

			// Check EOF
			if(read==-1)
			{
				byte[] trimmed=new byte[pos];
				System.arraycopy(buffer,0,trimmed,0,pos);
				is.close();
				return trimmed;
			}

			// Advance position in buffer
			pos+=read;

			// Enlarge buffer if needed
			if(pos==buffer.length)
			{
				byte[] newBuffer=new byte[buffer.length*2];
				System.arraycopy(buffer,0,newBuffer,0,buffer.length);
				buffer=newBuffer;
			}
		}
	}

	/**
	 * Loads a UTF-8 string from an input stream, then closes it.
	 * @param is Stream to read
	 * @return String
	 * @throws IOException Any I/O error
	 */
	public static String loadString(InputStream is) throws IOException
	{
		return new String(loadBytes(is),"UTF-8");
	}

	/**
	 * Escapes special characters in a string (angle brackets, ampersands, both
	 * types of quote) so that it can be included in the text of an XML element
	 * or in an attribute.
	 * <p>
	 * Note that any control characters in the string will turn into
	 * &lt;controlchar num='3'&gt; as these cannot be represented in XML.
	 * @param s String to escape
	 * @return XML-escaped version of string
	 */
	public static String esc(String s)
	{
		StringBuffer sb=new StringBuffer();
		for(int i=0;i<s.length();i++)
		{
			char c=s.charAt(i);
			if(c<32 && c!=9 && c!=10 && c!=13)
			{
				sb.append("<controlchar num='" + (int)c + "'/>");
				continue;
			}
			switch(c)
			{
			case '<' : sb.append("&lt;"); break;
			case '&' : sb.append("&amp;"); break;
			case '\'': sb.append("&apos;"); break;
			case '"' : sb.append("&quot;"); break;
			default: sb.append(c); break;
			}
		}
		return sb.toString();
	}

	/**
	 * @param string String to hash
	 * @return SHA-1 hash of strung
	 * @throws NoSuchAlgorithmException If Java is missing the SHA-1 provider
	 */
	public static String hash(String string) throws NoSuchAlgorithmException
	{
		// Get bytes
		byte[] hashDataBytes;
		try
		{
			hashDataBytes = string.getBytes("UTF-8");
		}
		catch(UnsupportedEncodingException e)
		{
			throw new Error("No UTF-8 support?!", e);
		}
		return hash(hashDataBytes);
	}

	/**
	 * @param hashDataBytes Data to hash
	 * @return SHA-1 hash of strung
	 * @throws NoSuchAlgorithmException If Java is missing the SHA-1 provider
	 */
	public static String hash(byte[] hashDataBytes) throws NoSuchAlgorithmException
	{
		// Hash data and return 40-character string
		MessageDigest m;
		m = MessageDigest.getInstance("SHA-1");
		m.update(hashDataBytes, 0, hashDataBytes.length);
		String sha1 = new BigInteger(1, m.digest()).toString(16);
		while(sha1.length() < 40)
		{
			sha1 = "0" + sha1;
		}
		return sha1;
	}
}
