package org.f4g.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Class to collect the configuration items. Its properties can be accessed in a static way
 * throughout the framework
 * 
 * @author FIT4Green
 *
 */
public class Configuration {

	private static Properties configuration = null;
	
	public Configuration(String filePath) throws IOException {
		configuration = new Properties();
		ClassLoader cl = this.getClass().getClassLoader();
		InputStream is = cl.getResourceAsStream(filePath);
		configuration.load(is);
	}
	
	public static String get(String key){
		return configuration.getProperty(key);
	}
	
}
