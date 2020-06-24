package com.staking.utils;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FileUtils {

    public static void saveToFile(String filePath, String content) throws IOException {
        Path path = Paths.get(filePath);
        if(!Files.exists(path.getParent())) Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }
    public static void appendToFile(String filePath, String content) throws IOException {
        Path path = Paths.get(filePath);
        if(!Files.exists(path.getParent())) Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
    public static String getContent(String filePath) throws IOException {
        return Files.readString(Paths.get(filePath));
    }
    public static void rewriteToFile(String filePath, String content) throws IOException {
        Path path = Paths.get(filePath);
        if(!Files.exists(path.getParent())) Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static Path getFirstFileByRegexp(String directoryPath, String regexp){
        try {
            Iterator<Path> iterator = Files.newDirectoryStream(Path.of(directoryPath), regexp).iterator();
            return iterator.hasNext()?iterator.next():null;
        }catch (Exception e){
            return null;
        }
    }

    public static String readFromFilesystem(Path pathStr){
        try {
            CharsetMatch cm;
            try(InputStream input = Files.newInputStream(pathStr)) {
                BufferedInputStream bis = new BufferedInputStream(input);
                CharsetDetector cd = new CharsetDetector();
                cd.setText(bis);
                cm = cd.detect();
            } catch (IOException e){
                Thread.sleep(2000L);
                return readFromFilesystem(pathStr, true);
            }
            return Files.readString(pathStr, Charset.forName(cm.getName()));
        }catch (Exception e){
            throw new RuntimeException("Can't read file "+pathStr, e);
        }
    }

    private static String readFromFilesystem(Path pathStr, boolean retry){
        try {
            CharsetMatch cm;
            try(InputStream input = Files.newInputStream(pathStr)) {
                BufferedInputStream bis = new BufferedInputStream(input);
                CharsetDetector cd = new CharsetDetector();
                cd.setText(bis);
                cm = cd.detect();
            } catch (IOException e){
                if (retry) throw new RuntimeException(e);
                else return readFromFilesystem(pathStr, true);
            }
            return Files.readString(pathStr, Charset.forName(cm.getName()));
        }catch (Exception e){
            throw new RuntimeException("Can't read file "+pathStr, e);
        }
    }

    public static String readFromFilesystem(String pathStr){
        return readFromFilesystem(Path.of(pathStr));
    }

    public static List<String> readListString(String path){
        List<String> result = new ArrayList<>();
        try {
            result = Files.readAllLines(Path.of(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}
