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
            this.resourceLoader = isARM() || !isFiji() ? new MacOsXDylibLoader() : new OldMacOsDyLibLoader();
        }
        if (LibraryLoader.getOS().startsWith("Linux")) {
            this.resourceLoader = new LinuxOsLoader();
        }
    }

    private boolean isFiji() {
        return IJ.getInstance() != null && IJ.getInstance().getInfo().contains("Fiji");
    }

    private boolean isARM() {
        return LibraryLoader.getOSArch().contains("aarch64");
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
            try (OutputStream out = FileUtils.openOutputStream(fileOut)) {
                if (in != null) {
                    IOUtils.copy(in, out);
                    in.close();
                    out.close(); // Without this line it doesn't work on windows, so, just leave it there, avoid even the check for the OS
                    System.load(fileOut.toString());
                }
            }
        } catch (Exception e) {
            IJ.showMessage(e.getMessage());
        }
    }
}
