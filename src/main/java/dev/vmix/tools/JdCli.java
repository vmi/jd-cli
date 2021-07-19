package dev.vmix.tools;

import dev.vmix.decompiler.ClassFileScanner;
import dev.vmix.decompiler.DecompilerLoader;
import dev.vmix.decompiler.DecompilerPrinter;
import org.jd.core.v1.ClassFileToJavaSourceDecompiler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

public class JdCli {

    private static String HELP = "JD-CLI version %s%n" +
        "%n" +
        "Usage: %s [OPTIONS] TARGET ...%n" +
        "%n" +
        "[TARGET]%n" +
        "* class file%n" +
        "* class directory%n" +
        "* jar file%n" +
        "%n" +
        "[OPTIONS]%n" +
        "-cp, --classpath CLASSPATH  Specify CLASSPATH.%n" +
        "-o,  --out-dir OUTDIR       Specify the output directory of generated Java source.%n" +
        "                            (default: standard output)%n" +
        "-h, --help                  Show this message.%n";

    private static void help() {
        String ver = "*.*.*";
        InputStream is = null;
        try {
            Properties pom = new Properties();
            is = JdCli.class.getResourceAsStream("/META-INF/maven/dev.vmix.utils/jd-cli/pom.properties");
            if (is != null) {
                pom.load(is);
                ver = pom.getProperty("version", "*.*.*");
            }
        } catch (IOException e) {
            // ignore
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        String cmd = System.getenv("COMMAND");
        if (cmd == null)
            cmd = "jd-cli";
        System.err.printf(HELP, ver, cmd);
        System.exit(1);
    }

    private static class ParsedArgs {

        List<Path> classPaths = new ArrayList<>();
        List<String> args = new ArrayList<>();
        Path outDir;
        boolean isVerbose = false;
        boolean isHelp = false;

        ParsedArgs(String... args) {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                case "-cp":
                case "--classpath":
                    String arg = args[++i];
                    String[] cps = arg.split(Pattern.quote(File.pathSeparator));
                    for (String cp : cps)
                        classPaths.add(Paths.get(cp));
                    break;
                case "-o":
                case "--out-dir":
                    outDir = Paths.get(args[++i]);
                    break;
                case "-v":
                case "--verbose":
                    isVerbose = true;
                    break;
                case "-h":
                case "--help":
                    isHelp = true;
                    break;
                default:
                    this.args.add(args[i]);
                    break;
                }
            }
        }
    }

    public static void main(String... args) throws IOException {
        if (args.length == 0) {
            help();
            return; // do not reach here.
        }
        ParsedArgs parsedArgs = new ParsedArgs(args);
        if (parsedArgs.isHelp) {
            help();
            return;
        }
        // do decompile.
        try (ClassFileScanner scanner = new ClassFileScanner()) {
            System.err.println("### Listing targets.");
            DecompilerLoader loader = new DecompilerLoader(parsedArgs.classPaths);
            List<String> targets = new ArrayList<>();
            for (String arg : parsedArgs.args) {
                scanner.scan(arg, (internalName, path) -> {
                    if (path != null)
                        loader.put(internalName, path);
                    if (internalName.indexOf('$') < 0)
                        targets.add(internalName);
                });
            }
            Collections.sort(targets);
            System.err.println("### Decompiling targets.");
            ClassFileToJavaSourceDecompiler decompiler = new ClassFileToJavaSourceDecompiler();
            for (String target : targets) {
                DecompilerPrinter printer = new DecompilerPrinter();
                try {
                    decompiler.decompile(loader, printer, target);
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
                String src = printer.toString();
                if (parsedArgs.outDir == null) {
                    System.out.println("// BEGIN: [" + target + ".java]");
                    System.out.println(src);
                    System.out.println("// END: [" + target + ".java]");
                    System.out.println();
                } else {
                    Path outPath = parsedArgs.outDir.resolve(target + ".java");
                    Files.createDirectories(outPath.getParent());
                    try (BufferedWriter bw = Files.newBufferedWriter(outPath)) {
                        System.err.println("### Generating: " + outPath);
                        bw.write(src);
                    }
                }
            }
        }
    }
}
