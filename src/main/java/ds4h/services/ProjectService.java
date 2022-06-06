package ds4h.services;


import ds4h.image.model.Project;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class ProjectService {
    private final static String ROIS_AND_INDEXES = "rois_and_indexes";

    private ProjectService() {}

    public static Project load() {
        Project project;
        final Optional<String> zipFile = FileService.promptForFile((dir, name) -> name.endsWith(".zip"));
        if (!zipFile.isPresent()) {
            return null;
        }
        final String unzippedFile = ZipService.extract(zipFile.get()).getAbsolutePath();
        final Set<String> files = FileService.getAllFiles(unzippedFile);
        project = extractFilePaths(files, XMLService.loadProject(files.stream().filter(file -> file.endsWith("xml")).findFirst().get()));
        return project;
    }

    private static Project extractFilePaths(Set<String> files, Project project) {
        List<String> filePaths = files.stream().filter(file -> !file.endsWith("xml")).sorted().sorted(Comparator.comparing(item -> {
            for (String filePath : project.getFilePaths()) {
                if (filePath.endsWith(item)) {
                    return 1;
                }
                return 0;
            }
            return -1;
        })).collect(Collectors.toList());
        project.setFilePaths(filePaths);

        return project;
    }

    public static void save(Project project, String outputPath) {
        String xmlPath = outputPath + File.separator + "rois_with_index.xml";
        XMLService.create(ROIS_AND_INDEXES, "image", project, xmlPath);
        List<String> files = new ArrayList<>(project.getFilePaths());
        files.add(xmlPath);
        ZipService.zipIt(outputPath, files);
    }
}
