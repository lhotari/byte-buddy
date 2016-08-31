package net.bytebuddy.build.gradle;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import com.google.common.io.Files;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.Assert.assertEquals;

public class ByteBuddyPluginFunctionalTest {
	@Rule public final TemporaryFolder testProjectDir = new TemporaryFolder();
	private File buildFile;

	@Before
	public void setup() throws IOException {
		buildFile = testProjectDir.newFile("build.gradle");
	}

	@Test
	public void testByteBuddy() throws IOException {
		Files.write("plugins { id 'java'\nid 'net.bytebuddy.byte-buddy'\n}", buildFile,
				Charset.defaultCharset());
		Files.write(
				"public class Hello { public static void main(String[] args) { System.out.println(\"Hello world!\"); } }",
				new File(testProjectDir.newFolder("src", "main", "java"), "Hello.java"),
				Charset.defaultCharset());

		BuildResult result = GradleRunner.create().withPluginClasspath()
				.withProjectDir(testProjectDir.getRoot()).withArguments("-s", "classes")
				.build();

		assertEquals(result.task(":classes").getOutcome(), SUCCESS);
	}
}
