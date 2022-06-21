package ds4h.services;


import ds4h.image.model.Project;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class ProjectService {
    private static final String ROIS_AND_INDEXES = "rois_and_indexes";

    private ProjectService() {}

    public static Project load() {
        Project project = null;
        final Optional<String> zipFile = FileService.promptForFile((dir, name) -> name.endsWith(".zip"));
        if (!zipFile.isPresent()) {
            return null;
        }
        final String unzippedFile = ZipService.extract(zipFile.get()).getAbsolutePath();
        final Set<String> files = FileService.getAllFiles(unzippedFile);
        Optional<String> xmlPath = files.stream().filter(file -> file.endsWith("xml")).findFirst();
        if (xmlPath.isPresent()) {
            project = extractFilePaths(files, XMLService.loadProject(xmlPath.get()));
        }
        return project;
    }

    private static Project extractFilePaths(Set<String> files, Project project) {
        List<String> filePaths = files.stream().filter(file -> !file.endsWith("xml")).sorted().sorted(Comparator.comparing(item -> {
            int value = 0;
            for (String filePath : project.getFilePaths()) {
                if (filePath.endsWith(item)) {
                    value = 1;
                    break;
                }
            }
            return value;
        })).collect(Collectors.toList());
        project.setFilePaths(filePaths);

        return project;
    }

    public static void save(Project project, String imagesDir, String outputPath) {
        String xmlPath = imagesDir + File.separator + "rois_with_index.xml";
        XMLService.create(ROIS_AND_INDEXES, "image", project, xmlPath);
        List<String> files = new ArrayList<>(project.getFilePaths());
        files.add(xmlPath);
        ZipService.zipIt(outputPath, files);
    }
}
