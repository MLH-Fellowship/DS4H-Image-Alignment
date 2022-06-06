package ds4h.services;


import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static ds4h.services.FileService.newFile;

public class ZipService {
    private final static String DESKTOP_PATH = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "ds4hProjects" + File.separator;

    private ZipService() {}

    public static File extract(String filePath) {
        File destDir = new File(DESKTOP_PATH + getToday() + File.separator);
        byte[] buffer = new byte[1024];
        ZipInputStream zis;
        try {
            zis = new ZipInputStream(new FileInputStream(filePath));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        ZipEntry zipEntry;
        try {
            zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(destDir, zipEntry);
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    // fix for Windows-created archives
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }
                    // write file content
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return destDir;
    }

    private static String getToday() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return LocalDate.now().format(formatter);
    }

    public static void zipIt(String dirPath, List<String> files) {
        String dirName = dirPath + File.separator + getToday() + ".zip";
        List<String> srcFiles = new ArrayList<>(files);
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(dirName);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        for (String srcFile : srcFiles) {
            File fileToZip = new File(srcFile);
            FileInputStream fis;
            try {
                fis = new FileInputStream(fileToZip);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
            try {
                zipOut.putNextEntry(zipEntry);
                byte[] bytes = new byte[1024];
                int length;
                while((length = fis.read(bytes)) >= 0) {
                    zipOut.write(bytes, 0, length);
                }
                fis.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
        try {
            zipOut.close();
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
