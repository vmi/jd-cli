package dev.vmix.decompiler;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;

public class ClassFileScanner implements Closeable {

    private static final String EXT_CLASS = ".class";
    private static final int EXT_CLASS_LEN = EXT_CLASS.length();

    private static final Logger log = Logger.getLogger(ClassFileScanner.class.getCanonicalName());

    private final List<FileSystem> fileSystems = new ArrayList<>();

    public ClassFileScanner() {
    }

    private static String getExt(String filename) {
        int i = filename.lastIndexOf('.');
        return i >= 0 ? filename.substring(i) : null;
    }

    private static Path getBaseDir(Path path, String internalName) {
        int index = -1;
        do {
            path = path.getParent();
        } while ((index = internalName.indexOf('/', index + 1)) >= 0);
        return path;
    }

    private static String getInternalName(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            JavaClass javaClass = new ClassParser(is, path.toString()).parse();
            return javaClass.getClassName().replace('.', '/');
        } catch (IOException e) {
            throw new DecompilerException(e);
        }
    }

    private void scanDir(Path dir, BiConsumer<String, Path> handler) {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                private Path baseDir = null;

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String ext = getExt(file.toString());
                    if (EXT_CLASS.equals(ext)) {
                        String internalName = null;
                        if (baseDir != null) {
                            String relPath = baseDir.relativize(file).toString().replace('\\', '/');
                            if (!relPath.startsWith("../"))
                                internalName = relPath.substring(0, relPath.length() - EXT_CLASS_LEN);
                        }
                        if (internalName == null) {
                            internalName = getInternalName(file);
                            baseDir = getBaseDir(file, internalName);
                        }
                        handler.accept(internalName, file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void scan(String filename, BiConsumer<String, Path> handler) {
        Path path = Paths.get(filename);
        if (Files.isRegularFile(path)) {
            String ext = getExt(filename);
            switch (ext) {
            case ".class":
                String internalName = getInternalName(path);
                handler.accept(internalName, path);
                break;
            case ".jar":
            case ".war":
            case ".ear":
            case ".zip":
                try {
                    FileSystem fs = FileSystems.newFileSystem(path, null);
                    fileSystems.add(fs);
                    for (Path root : fs.getRootDirectories())
                        scanDir(root, handler);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                break;
            default:
                log.info(() -> String.format("[SKIP] unexpected file: %s", filename));
                return;

            }
        } else if (Files.isDirectory(path)) {
            scanDir(path, handler);
        } else {
            // FQCN(package.name.ClassName) or internal name(package/name/ClassName)
            String internalName = filename.replace('.', '/');
            handler.accept(internalName, null);
        }
    }

    @Override
    public void close() throws IOException {
        for (FileSystem fs : fileSystems) {
            try {
                fs.close();
            } catch (IOException e) {
                log.warning(() -> e.toString());
            }
        }
    }
}
