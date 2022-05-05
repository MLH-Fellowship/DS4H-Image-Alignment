package ds4h.services.loader;

import ds4h.image.registration.ImageAlignment;

import java.io.InputStream;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class MacOsXDylibLoader implements ResourceLoader {
    private static final String PREFIX = "libopencv_";
    private static final String EXT = ".dylib";

    @Override
    public List<InputStream> getInputStreams() {
        return OpenCVUtility
                .loadLibraries(PREFIX, EXT)
                .stream()
                .map(ImageAlignment.class::getResourceAsStream)
                .collect(toList());
    }

    @Override
    public String getExt() {
        return EXT;
    }
}
