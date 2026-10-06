package br.car.dsp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Paths to the About page index JSON and the directory holding its markdown tab content.
 * Source of truth: dsp-backend/config/about/
 * Accepts file:… or an absolute/relative filesystem path.
 */
@ConfigurationProperties(prefix = "dsp.about-config")
public class AboutConfigProperties {

	/**
	 * Example: file:/config/about-config.json (Compose)
	 * or file:config/about/about-config.json (local bootRun from repo root)
	 */
	private String configFile = "file:config/about/about-config.json";

	/**
	 * Directory containing the markdown files referenced by each tab's "file".
	 * Example: file:/config/about/ (Compose)
	 * or file:config/about/ (local bootRun from repo root)
	 */
	private String contentDir = "file:config/about/";

	public String getConfigFile() {
		return configFile;
	}

	public void setConfigFile(String configFile) {
		this.configFile = configFile;
	}

	public String getContentDir() {
		return contentDir;
	}

	public void setContentDir(String contentDir) {
		this.contentDir = contentDir;
	}
}
