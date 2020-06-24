package com.staking;

import com.staking.utils.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

public class FileUtilsTest {

    @Test
    public void writeToFile() throws IOException {
        Path of = Path.of("/home/andrii/test/123.txt");

        FileUtils.rewriteToFile("/home/andrii/test/123/123.txt", "1235");
    }
}
