package com.shuqi.findbugs;

import edu.umd.cs.findbugs.SystemProperties;

public class DetectorConfig {	
	
	static {
		try {
			SystemProperties.loadPropertiesFromURL(DetectorConfig.class.getResource("/config.properties"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String getProperty(String key) {
		return SystemProperties.getProperty(key);
	}
}
