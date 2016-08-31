package net.bytebuddy.build.gradle;

import java.io.File;

public class Transformation {
	private String plugin;

	private Iterable<File> classpathFiles;

	public Transformation() {

	}

	public String getPlugin() {
		return plugin;
	}

	public void setPlugin(String plugin) {
		this.plugin = plugin;
	}

	public Iterable<File> getClasspathFiles() {
		return classpathFiles;
	}

	public void setClasspathFiles(Iterable<File> classpathFiles) {
		this.classpathFiles = classpathFiles;
	}
}
