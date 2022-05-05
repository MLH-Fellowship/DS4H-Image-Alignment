package ds4h.services.loader;

import ij.IJ;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

public class Loader implements LibraryLoader {
    private ResourceLoader resourceLoader;

    public Loader() {
        if (LibraryLoader.getOS().startsWith("Windows")) {
            this.resourceLoader = new WindowsDllLoader();
        }
        if (LibraryLoader.getOS().startsWith("Mac OS")) {
            this.resourceLoader = new MacOsXDylibLoader();
        }
    }

    @Override
    public void load() {
        try {
            this.resourceLoader.getInputStreams().forEach(this::handleLoad);
        } catch (Exception exception) {
            IJ.showMessage(exception.getMessage());
        }
    }

    private void handleLoad(InputStream in) {
        try {
            File fileOut = File.createTempFile("lib", this.resourceLoader.getExt());
            OutputStream out = FileUtils.openOutputStream(fileOut);
            if (in != null) {
                IOUtils.copy(in, out);
                in.close();
                out.close();
                System.load(fileOut.toString());
            }
        } catch (Exception e) {
            IJ.showMessage(e.getMessage());
        }
    }
}
