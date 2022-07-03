package ds4h.services;


import ds4h.image.model.Project;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ProjectService {
    private static final String ROIS_AND_INDEXES = "rois_and_indexes";

    private ProjectService() {
    }

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
            project = XMLService.loadProject(xmlPath.get());
        }
        return project;
    }

    public static void save(Project project, String imagesDir, String outputPath) {
        String xmlPath = imagesDir + File.separator + "rois_with_index.xml";
        XMLService.create(ROIS_AND_INDEXES, "image", project, xmlPath);
        List<String> files = new ArrayList<>(project.getFilePaths());
        files.add(xmlPath);
        try {
            ZipService.zipIt(outputPath, files);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
