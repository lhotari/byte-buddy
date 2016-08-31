package net.bytebuddy.build.gradle;

import java.io.File;
import java.io.IOException;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.compile.AbstractCompile;

public class ByteBuddyPlugin implements Plugin<Project> {

    /*
	 * TODO:
     *
     * 1. Create complex configuration (allow nesting and lists).
     * 2. Find way to append task to class compilation / test-class compilation.
     * 3. Integrate Aether and read (Maven?) repositories from user configuration.
     * 4. Read class path / test class path.
     *
     * Bonus:
     * 1. Read explicit dependencies from Maven POM file instead of duplication in build.gradle.
     * 2. Are main and test folders of build target customizable locations?
     * 3. What GradleExceptions should be thrown? Any sub types?
     */

	@Override
	public void apply(final Project project) {
		final ByteBuddyExtension byteBuddyExtension = project.getExtensions()
				.create("byteBuddy", ByteBuddyExtension.class, project);
		project.getTasks().withType(AbstractCompile.class, new Action<AbstractCompile>() {
			@Override
			public void execute(AbstractCompile compileTask) {
				final Iterable<File> compileClasspathFiles = compileTask.getClasspath();
				final File classesDir = compileTask.getDestinationDir();
				compileTask.doLast(new Action<Task>() {
					@Override
					public void execute(Task task) {
						try {
							new Transformer(project, byteBuddyExtension)
									.processOutputDirectory(classesDir,
											compileClasspathFiles);
						}
						catch (IOException e) {
							throw new GradleException(
									"Exception in byte-buddy processing", e);
						}
					}
				});
			}
		});
	}
}
