package com.jfinal.ext.plugin.cron;

import it.sauronsoftware.cron4j.Scheduler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

import com.jfinal.log.Logger;
import com.jfinal.plugin.IPlugin;

public class Cron4jPlugin implements IPlugin {
	
	protected final Logger logger = Logger.getLogger(getClass());
	
	private Scheduler scheduler = null;
	private String config = "job.properties";
	private Properties properties;

	public Cron4jPlugin(String config) {
		this.config = config;
	}

	public Cron4jPlugin() {
	}

	@Override
	public boolean start() {
		scheduler = new Scheduler();
		loadProperties();
		Enumeration<Object> enums = properties.keys();
		while (enums.hasMoreElements()) {
			String key = enums.nextElement() + "";
			if (!key.endsWith("job")) {
				continue;
			}
			if (!isEnableJob(enable(key))) {
				continue;
			}
			String jobClassName = properties.get(key) + "";
			String jobCronExp = properties.getProperty(cronKey(key)) + "";
			Class<Runnable> clazz;
			try {
				clazz = (Class<Runnable>) Class.forName(jobClassName);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
			try {
				scheduler.schedule(jobCronExp,clazz.newInstance());
			} catch (Exception e) {
				logger.error(e.getMessage(),e);
				continue;
			}
			logger.debug(jobClassName + " has been scheduled to run and repeat based on expression: "+jobCronExp);
		}
		scheduler.start();
		return true;
	}

	private String enable(String key) {
		return key.substring(0, key.lastIndexOf("job")) + "enable";
	}

	private String cronKey(String key) {
		return key.substring(0, key.lastIndexOf("job")) + "cron";
	}

	private boolean isEnableJob(String enableKey) {
		Object enable = properties.get(enableKey);
		if (enable != null && "false".equalsIgnoreCase((enable + "").trim())) {
			return false;
		}
		return true;
	}

	private void loadProperties() {
		properties = new Properties();
		logger.debug("config is: "+config);
		InputStream is = Cron4jPlugin.class.getClassLoader()
				.getResourceAsStream(config);
		try {
			properties.load(is);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		logger.debug("------------load Propteries---------------");
		logger.debug(properties.toString());
		logger.debug("------------------------------------------");
	}

	@Override
	public boolean stop() {
		scheduler.stop();
		return true;
	}
}
