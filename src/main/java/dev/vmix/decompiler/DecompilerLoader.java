package dev.vmix.decompiler;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.loader.LoaderException;

public class DecompilerLoader implements Loader {

    private final List<Path> classPaths;
    private final Map<String, Object> classMap = new HashMap<>();

    public DecompilerLoader(List<Path> classPaths) {
        this.classPaths = new ArrayList<>(classPaths);
    }

    public void put(String internalName, Path path) {
        if (!classMap.containsKey(internalName))
            classMap.put(internalName, path);
    }

    @Override
    public byte[] load(String internalName) throws LoaderException {
        Object object = classMap.get(internalName);
        if (object instanceof byte[]) {
            return (byte[]) object;
        }
        try {
            Path path;
            if (object instanceof Path) {
                path = (Path) object;
            } else if (object instanceof URL) {
                path = Paths.get(((URL) object).toURI());
            } else {
                throw new LoaderException("Unexpected object: " + object);
            }
            byte[] content = Files.readAllBytes(path);
            classMap.put(internalName, content);
            return content;
        } catch (URISyntaxException | IOException e) {
            throw new LoaderException(e);
        }
    }

    @Override
    public boolean canLoad(String internalName) {
        Object object = classMap.get(internalName);
        if (object!=null)
            return true;
        String resName = internalName + ".class";
        for (Path classPath : classPaths) {
            Path path = classPath.resolve(resName);
            if (Files.isRegularFile(path)) {
                classMap.put(internalName, path);
                return true;
            }
        }
        URL r = getClass().getResource("/" + resName);
        if (r != null) {
            classMap.put(internalName, r);
            return true;
        }
        return false;
    }
}
