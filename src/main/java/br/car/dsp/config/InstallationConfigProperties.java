package br.car.dsp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Path to the installation JSON (labels, screens, KPIs).
 * Source of truth: dsp-backend/config/installation/installation-config.json
 * Accepts file:… or an absolute/relative filesystem path.
 */
@ConfigurationProperties(prefix = "dsp.installation-config")
public class InstallationConfigProperties {

	/**
	 * Example: file:/config/installation-config.json (Compose)
	 * or file:config/installation/installation-config.json (local bootRun from repo root)
	 */
	private String file = "file:config/installation/installation-config.json";

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}
}
