package net.bytebuddy.build.gradle;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

public class ClassLoaderResolver implements Closeable {
	private final Logger logger;
	private final Map<String, ClassLoader> classLoaders;

	public ClassLoaderResolver(Project project) throws MalformedURLException {
		this.logger = project.getLogger();
		this.classLoaders = new HashMap<String, ClassLoader>();
	}

	public ClassLoader resolve(String transformerId, Iterable<File> classpathFiles) {
		ClassLoader classLoader = classLoaders.get(transformerId);
		if (classLoader == null) {
			classLoader = createClassLoader(classpathFiles);
			classLoaders.put(transformerId, classLoader);
		}
		return classLoader;
	}

	private ClassLoader createClassLoader(Iterable<File> classpathFiles) {
		List<URL> classpathElements = new ArrayList<URL>();
		for (File file : classpathFiles) {
			try {
				classpathElements.add(file.toURI().toURL());
			}
			catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
		return new URLClassLoader(classpathElements.toArray(new URL[0]));
	}

	@Override
	public void close() throws IOException {
		for (ClassLoader classLoader : classLoaders.values()) {
			if (classLoader instanceof Closeable) { // URLClassLoaders are only closeable since Java 1.7.
				((Closeable) classLoader).close();
			}
		}
	}
}
