package refdiff.test.util;

import refdiff.parsers.python.PythonPlugin;

public class PythonParserSingleton {
	
	private static PythonPlugin instance = null;
	
	public static PythonPlugin get() {
		try {
			if (instance == null) {
				instance = new PythonPlugin();
			}
			return instance; 
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
