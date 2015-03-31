package com.tresmonos.calendar;

import android.content.Context;


import org.jcaki.Strings;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyManager {

	public static String PARSE_APPLICATION_ID = "parseApplicationId";
	public static String PARSE_CLIENT_KEY = "parseClientKey";

	private static Properties instance = null;

	public static String getPropertyValue(Context context, String key) {
		if (instance == null) {
			synchronized (PropertyManager.class) {
				if (instance == null) {
					instance = new Properties();
					try {
						InputStream inputStream = context.getAssets().open("config.properties");
						instance.load(inputStream);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}

		String value = instance.getProperty(key);
		if (Strings.isNullOrEmpty(value)) {
			throw new RuntimeException(String.format("Unable to find value for property %s", key));
		}
		return value;
	}
}

