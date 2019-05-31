/*
The MIT license (MIT).

Copyright © 2014 Florian Schmaus <flo@geekplace.eu>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package eu.geekplace.iesp;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import android.content.SharedPreferences;
import android.util.Xml;

public class ImportExportSharedPreferences {

	private static final String BOOLEAN = Boolean.class.getSimpleName();
	private static final String FLOAT = Float.class.getSimpleName();
	private static final String INTEGER = Integer.class.getSimpleName();
	private static final String LONG = Long.class.getSimpleName();
	private static final String STRING = String.class.getSimpleName();

	private static final String NAME = "name";
	private static final String PREFERENCES = "preferences";

	public static void exportToFile(SharedPreferences sharedPreferences, File outFile,
			Set<String> doNotExport) throws IOException {
		export(sharedPreferences, new FileWriter(outFile), doNotExport);
	}

	/**
	 * sharedPreferences must be API 8 compatible. That is, it must not contain
	 * StringSets.
	 * 
	 * @param sharedPreferences
	 * @param writer
	 * @param doNotExport
	 *            a set of keys that should not get exported (passwords, etc.),
	 *            may be null
	 */
	public static void export(SharedPreferences sharedPreferences, Writer writer,
			Set<String> doNotExport) throws IOException {
		XmlSerializer serializer = Xml.newSerializer();
		serializer.setOutput(writer);
		serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
		serializer.startDocument("UTF-8", true);
		serializer.startTag("", PREFERENCES);
		for (Map.Entry<String, ?> entry : sharedPreferences.getAll().entrySet()) {
			String key = entry.getKey();
			if (doNotExport != null && doNotExport.contains(key)) continue;

			Object valueObject = entry.getValue();
			// Skip this entry if the value is null;
			if (valueObject == null) continue;

			String valueType = valueObject.getClass().getSimpleName();
			String value = valueObject.toString();
			serializer.startTag("", valueType);
			serializer.attribute("", NAME, key);
			serializer.text(value);
			serializer.endTag("", valueType);

		}
		serializer.endTag("", PREFERENCES);
		serializer.endDocument();
		writer.close();
	}

	public static boolean importFromFile(SharedPreferences sharedPreferences, File inFile)
			throws Exception {
		return importFromReader(sharedPreferences, new FileReader(inFile));
	}

	/**
	 * 
	 * @param sharedPreferences
	 * @param in
	 * @return
	 * @throws Exception
	 */
	public static boolean importFromReader(SharedPreferences sharedPreferences, Reader in)
			throws Exception {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(in);
		int eventType = parser.getEventType();
		String name = null;
		String key = null;
		while (eventType != XmlPullParser.END_DOCUMENT) {
			switch (eventType) {
			case XmlPullParser.START_TAG:
				name = parser.getName();
				key = parser.getAttributeValue("", NAME);
				break;
			case XmlPullParser.TEXT:
				// The parser is reading text outside an element if name is null,
				// so simply ignore this text part (which is usually something like '\n')
				if (name == null) break;
				String text = parser.getText();
				if (BOOLEAN.equals(name)) {
					editor.putBoolean(key, Boolean.parseBoolean(text));
				} else if (FLOAT.equals(name)) {
					editor.putFloat(key, Float.parseFloat(text));
				} else if (INTEGER.equals(name)) {
					editor.putInt(key, Integer.parseInt(text));
				} else if (LONG.equals(name)) {
					editor.putLong(key, Long.parseLong(text));
				} else if (STRING.equals(name)) {
					editor.putString(key, text);
				} else if (!PREFERENCES.equals(name)) {
					throw new Exception("Unkown type " + name);
				}
				break;
			case XmlPullParser.END_TAG:
				name = null;
				break;
			}
			eventType = parser.next();
		}
		return editor.commit();
	}
}
