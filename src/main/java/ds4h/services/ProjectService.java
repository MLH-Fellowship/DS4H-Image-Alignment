package ds4h.services;


import ds4h.image.model.Project;
import ds4h.image.model.ProjectImage;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
            project = XMLService.loadProject(xmlPath.get(), files);
        }
        return project;
    }

    public static void save(Project project, String imagesDir, String outputPath) {
        String xmlPath = imagesDir + File.separator + "rois_with_index.xml";
        XMLService.create(ROIS_AND_INDEXES, "image", project, xmlPath);
        List<String> files = project.getProjectImages().stream().map(ProjectImage::getFilePath).collect(Collectors.toList());
        files.add(xmlPath);
        try {
            ZipService.zipIt(outputPath, files);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
