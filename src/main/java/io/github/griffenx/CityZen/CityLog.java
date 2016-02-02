package io.github.griffenx.CityZen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CityLog {
	private String filename;
	private File file = null;
	private FileWriter writer;
	private boolean debug;
	
	public CityLog(String filepath) {
		filename = filepath;
		if (CityZen.getPlugin().getConfig().getBoolean("logEnabled")) {
			file = new File(filepath);
			debug = CityZen.getPlugin().getConfig().getBoolean("logDebug");
			try {
				if (!file.exists()) {
					file.mkdirs();
					file.createNewFile();
				}
				file.setWritable(true);
				writer = new FileWriter(file);
			} catch (IOException e) {
				CityZen.getPlugin().getLogger().severe("Error setting up log file.\n" + e.toString());
			}
		}
	}
	
	public void write(String text) {
		if (file != null) {
			try {
				String timeStamp = "[" + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()) + "]";
				writer.write(timeStamp + text + "\n");
				writer.flush();
			} catch (IOException e) {
				CityZen.getPlugin().getLogger().severe("Error writing to log file.\n" + e.toString());
			}
		}
	}
	
	public void debug(String text) {
		if (debug) write(text);
	}
	
	public String getFileName() {
		return filename;
	}
	
	public static String generateLogName() {
		return new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".log";
	}
}
