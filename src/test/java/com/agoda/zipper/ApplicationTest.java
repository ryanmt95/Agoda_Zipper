package com.agoda.zipper;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Cherednik
 * @since 20.02.2019
 */
@Test
public class ApplicationTest {

    private Path root;
    private Path srcDir;
    private Path destDir;
    private Path resDir;

    @BeforeMethod
    public void createDirectory() throws IOException {
        root = Files.createTempDirectory("zipper");
        srcDir = root.resolve("src");
        destDir = root.resolve("dest");
        resDir = destDir.resolve("res");

        Files.createDirectories(srcDir);
        Files.createDirectories(destDir);
        Files.createDirectories(resDir);
    }

    @BeforeMethod(dependsOnMethods = "createDirectory")
    public void copyTestData() throws IOException {
        Path dataDir = Paths.get("src/test/resources/data").toAbsolutePath();

        Files.walk(dataDir).forEach(path -> {
            try {
                if (Files.isDirectory(path))
                    Files.createDirectories(srcDir.resolve(dataDir.relativize(path)));
                else if (Files.isRegularFile(path))
                    Files.copy(path, srcDir.resolve(dataDir.relativize(path)));
            } catch(IOException e) {
                e.printStackTrace();
            }
        });

        Files.createDirectories(srcDir.resolve("empty_dir"));
    }

    @AfterMethod
    public void removeDirectory() throws IOException {
        FileUtils.deleteQuietly(root.toFile());
    }

    public void shouldZipAndUnzipWithSinglePart() throws Exception {
        Application.main("--add", srcDir.toString(), destDir.toString());
        Application.main("--extract", destDir.resolve("src.zip").toString(), destDir.resolve("res").toString());

        checkDestinationDir(1);
        checkResultDir();
    }

    public void shouldZipAndUnzipWithMultipleParts() throws Exception {
        Application.main("--add", srcDir.toString(), destDir.toString(), "1MB");
        Application.main("--extract", destDir.resolve("src.zip").toString(), destDir.resolve("res").toString());

        checkDestinationDir(10);
        checkResultDir();
    }

    private void checkDestinationDir(int totalParts) throws IOException {
        assertThat(Files.exists(destDir)).isTrue();
        assertThat(Files.isDirectory(destDir)).isTrue();
        assertThat(getRegularFilesAmount(destDir)).isEqualTo(totalParts);
        assertThat(getFoldersAmount(destDir)).isOne();

        assertThat(Files.exists(resDir)).isTrue();
        assertThat(getRegularFilesAmount(resDir)).isZero();
        assertThat(getFoldersAmount(resDir)).isOne();
    }

    private void checkResultDir() throws IOException {
        Path srcDir = resDir.resolve("src");
        assertThat(Files.exists(srcDir)).isTrue();
        assertThat(Files.isDirectory(srcDir)).isTrue();
        assertThat(getRegularFilesAmount(srcDir)).isEqualTo(5);
        assertThat(getFoldersAmount(srcDir)).isEqualTo(3);

        Path carDir = srcDir.resolve("car");
        assertThat(Files.exists(carDir)).isTrue();
        assertThat(Files.isDirectory(carDir)).isTrue();
        assertThat(getRegularFilesAmount(carDir)).isEqualTo(3);
        assertThat(getFoldersAmount(carDir)).isZero();

        Path starWarsDir = srcDir.resolve("Star Wars");
        assertThat(Files.exists(starWarsDir)).isTrue();
        assertThat(Files.isDirectory(starWarsDir)).isTrue();
        assertThat(getRegularFilesAmount(starWarsDir)).isEqualTo(4);
        assertThat(getFoldersAmount(starWarsDir)).isZero();

        Path emptyDir = srcDir.resolve("empty_dir");
        assertThat(Files.exists(emptyDir)).isTrue();
        assertThat(Files.isDirectory(emptyDir)).isTrue();
        assertThat(getRegularFilesAmount(emptyDir)).isZero();
        assertThat(getFoldersAmount(emptyDir)).isZero();
    }

    private static long getRegularFilesAmount(Path dir) throws IOException {
        return Files.list(dir).filter(path -> Files.isRegularFile(path)).count();
    }

    private static long getFoldersAmount(Path dir) throws IOException {
        return Files.list(dir).filter(path -> Files.isDirectory(path)).count();
    }


}
