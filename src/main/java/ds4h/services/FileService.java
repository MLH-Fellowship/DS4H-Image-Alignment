/*
 * COPYRIGHT:* Copyright (c) 2021, AUSL Romagna* Azienda USL della Romagna, Italy* All rights reserved.**
 * The informed consent published in integral part on the website of Azienda USL della Romagna
 * (Informed Consent AUSL, prot. N. 1683), must be citied.**
 * This material is free; you can redistribute it and/or modify it under the terms of the CC BY 4.0.*
 * This material is distributed WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package ds4h.services;

import ij.IJ;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileService {
    private static final String FILE_PROTOCOL = "file:" + File.separator + File.separator + File.separator;

    private FileService() {
    }

    public static List<String> promptForFiles() {
        FileDialog fileDialog = FileService.fileDialogHandler(true, false, null);
        return Arrays.stream(fileDialog.getFiles()).map(File::getPath).collect(Collectors.toList());
    }

    public static Optional<String> promptForFile(FilenameFilter filter) {
        FileDialog fileDialog = FileService.fileDialogHandler(false, false, filter);
        return Arrays.stream(fileDialog.getFiles()).map(File::getAbsolutePath).findFirst();
    }

    private static FileDialog fileDialogHandler(boolean isMultiple, boolean isSave, FilenameFilter filter) {
        FileDialog fileDialog = new FileDialog((Frame) null);
        if (filter != null) {
            fileDialog.setFilenameFilter(filter);
        }
        fileDialog.setMultipleMode(isMultiple);
        if (isSave) {
            fileDialog.setMode(FileDialog.SAVE);
        }
        fileDialog.setResizable(true);
        // Note: Other options must be put before this line, otherwise they won't be applied
        fileDialog.setVisible(true);
        return fileDialog;
    }

    public static Set<String> getFilesInJarFromDirectory(String directoryPath) throws IOException {
        final CodeSource src = FileService.class.getProtectionDomain().getCodeSource();
        final Set<String> files = new HashSet<>();
        if (src != null) {
            final URL jar = src.getLocation();
            try (ZipInputStream zip = new ZipInputStream(jar.openStream())) {
                ZipEntry zipEntry;
                while ((zipEntry = zip.getNextEntry()) != null) {
                    String entryName = zipEntry.getName();
                    if (entryName.contains(directoryPath) && !zipEntry.isDirectory()) {
                        files.add("/".concat(entryName));
                    }
                }
            }
        }
        return files;
    }

    public static Set<String> getAllFilesInDirectory(String directoryPath) {
        if (JarService.runningFromJar()) {
            try {
                return getFilesInJarFromDirectory(directoryPath);
            } catch (IOException e) {
                IJ.showMessage(e.getMessage());
            }
        }
        return Arrays.stream(Objects.requireNonNull(new File(directoryPath).listFiles()))
                .filter(File::isFile)
                .map(file -> file.toURI().toString())
                .collect(Collectors.toSet());
    }

    public static Set<String> getAllFiles(String directoryPath) {
        Set<String> files = new HashSet<>();
        try {
            Files.walk(Paths.get(directoryPath))
                    .filter(Files::isRegularFile)
                    .forEach(file -> files.add(file.toAbsolutePath().toString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return files;
    }

    public static String saveFile(File file, String directoryPath, boolean isImage) throws IllegalAccessException, IOException {
        final File homePathFile = new File(System.getProperty("user.home"));
        if (homePathFile.isDirectory() && !homePathFile.canWrite()) {
            throw new IllegalAccessException("Trying to save file in a protected area");
        }
        createDirectoryIfNotExist(directoryPath);
        File newFile = new File(directoryPath + File.separator + file.getName());
        if (isImage) {
            BufferedImage bufferedImage = ImageIO.read(file);
            ImageIO.write(bufferedImage, FileService.getFileExtension(file), newFile);
        }
        return FILE_PROTOCOL + newFile.getCanonicalPath();
    }

    public static void createDirectoryIfNotExist(String directoryPath) {
        final File directory = new File(directoryPath);
        if (directory.isDirectory() || directory.exists()) {
            return;
        }
        boolean mkdir = directory.mkdirs();
        if (!mkdir) {
            IJ.showMessage("Maybe you don't have the permission to do that");
        }
    }

    /**
     * https://stackoverflow.com/questions/3571223/how-do-i-get-the-file-extension-of-a-file-in-java
     *
     * @param file
     * @return
     */
    private static String getFileExtension(File file) {
        if (file == null) {
            return "";
        }
        final String name = file.getName();
        int i = name.lastIndexOf('.');
        return i > 0 ? name.substring(i + 1) : "";
    }

    public static void copyFile(String pathCopy, String pathPaste) throws IOException {
        // Due to the fact the language level is Java8 I must use Paths.get and not Path.of ( introduced in Java 11 )
        Path path = Paths.get(pathPaste);
        if (Files.exists(path)) {
            return;
        }
        if (JarService.runningFromJar()) {
            try (InputStream stream = FileService.class.getResourceAsStream(pathCopy)) {
                if (stream != null) {
                    Files.copy(stream, path);
                }
            }
        } else {
            Files.copy(Paths.get(pathCopy), path);
        }
    }

    public static void saveFile(String dialogTitle, String data, String ext) {
        final JFileChooser chooser = new JFileChooser(dialogTitle);
        int result = chooser.showSaveDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            String absolutePath = chooser.getSelectedFile().getAbsolutePath();
            if (!absolutePath.endsWith(ext)) {
                absolutePath += "." + ext;
            }
            try (FileWriter fw = new FileWriter(absolutePath)) {
                fw.write(data);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static String chooseDirectory() {
        final JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle("Choose a Directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(new Frame()) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().getAbsolutePath();
        }
        return System.getProperty("user.home") + "/Desktop";
    }

    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
        return destFile;
    }
}
