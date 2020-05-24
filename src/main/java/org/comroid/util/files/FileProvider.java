package org.comroid.util.files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.stream.IntStream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static java.io.File.pathSeparator;
import static java.io.File.separator;
import static java.io.File.separatorChar;

public class FileProvider {
    public final static Logger logger = LogManager.getLogger();
    private final static String PREFIX = "/var/dcb/cobalton/";

    public static String[] readContent(String subPath) throws NoSuchFileException {
        final File file = getFile(subPath);

        logger.info("Reading file [ " + fullPath(subPath) + " ]...");

        if (!file.exists())
            throw new NoSuchFileException(fullPath(subPath));

        try {
            return new BufferedReader(new FileReader(file))
                    .lines()
                    .toArray(String[]::new);
        } catch (FileNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    public static File getFile(String subPath) {
        final String path = fullPath(subPath).replace('/', separatorChar);
        logger.printf(Level.INFO, "Acquiring File [ %s ]", path);

        createDirs(path);

        final File file = new File(path);

        if (!file.exists()) {
            logger.printf(Level.WARN, "GET %s FAIL: File does not exist. Trying to create it...", path);

            try {
                if (!file.createNewFile()) {
                    logger.printf(Level.ERROR, "CREATE %s FAIL: Could not create File for unknown reason. Exiting.", path);
                    System.exit(1);
                    return null; // lol
                } else logger.printf(Level.INFO, "CREATE %s OK!", path);
            } catch (IOException e) {
                logger.printf(Level.ERROR, "CREATE %s FAIL: An [ %s ] occurred creating File. Exiting.", path, e.getClass().getSimpleName());
                e.printStackTrace(System.out);
                System.exit(1);
                return null; // lol
            }
        }

        return file;
    }

    public static String fullPath(String subPath) {
        return PREFIX + subPath;
    }

    private static void createDirs(final String forPath) {
        logger.printf(Level.INFO, "Checking directories for file [ %s ]...", forPath);

        final String[] paths = forPath.split(pathSeparator);

        if (paths.length <= 1) {
            logger.printf(Level.INFO, "CREATE PATHS %s OK! [ %d ]", forPath, paths.length - 1);
            return;
        }

        IntStream.range(0, paths.length)
                .mapToObj(value -> {
                    String[] myPath = new String[value];
                    System.arraycopy(paths, 0, myPath, 0, value);
                    return myPath;
                })
                .map(strs -> String.join(separator, strs))
                .filter(str -> !str.isEmpty())
                .forEachOrdered(path -> {
                    final File file = new File(path);

                    if (file.exists() && file.isDirectory())
                        return;

                    logger.printf(Level.ERROR, "GET PATH %s FAIL: Directory does not exist, trying to create it...", path);

                    if (file.mkdir())
                        logger.printf(Level.INFO, "CREATE PATH %s OK!", path);
                    else {
                        logger.printf(Level.ERROR, "CREATE PATH %s FAIL: Could not create directory for unknown reason.", path, forPath);
                        System.exit(1);
                    }
                });
    }
}
