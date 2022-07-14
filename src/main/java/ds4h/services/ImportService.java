package ds4h.services;

import ij.IJ;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;

import java.io.IOException;

public class ImportService {
    public static ImportProcess getProcessByFilePath(String filePath) throws IOException {
        final ImporterOptions options = new ImporterOptions();
        options.setId(filePath);
        options.setVirtual(true);
        options.setGroupFiles(false);
        options.setUngroupFiles(true);
        options.setOpenAllSeries(true);
        final ImportProcess process = new ImportProcess(options);
        try {
            process.execute();
        } catch (Exception e) {
            IJ.showMessage(e.getMessage());
        }
        return process;
    }
}
