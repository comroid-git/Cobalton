package de.comroid.util.files;

import java.io.File;
import java.io.IOException;
import java.util.stream.IntStream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static java.io.File.separator;
import static java.io.File.separatorChar;

public class FileProvider {
    public final static Logger logger = LogManager.getLogger();
    private final static String PREFIX = "/app/data/";

    public static File getFile(String subPath) {
        final String path = (PREFIX + subPath).replace('/', separatorChar);
        logger.printf(Level.INFO, "Acquiring File [ %s ]\n", path);

        createDirs(path);

        final File file = new File(path);

        if (!file.exists()) {
            logger.printf(Level.WARN, "File [ %s ] does not exist. Trying to create it...\n", path);

            try {
                if (!file.createNewFile()) {
                    logger.printf(Level.ERROR, " FAIL: Could not create File [ %s ] for unknown reason. Exiting.\n", path);
                    System.exit(1);
                    return null; // lol
                } else System.out.print(" OK!\n");
            } catch (IOException e) {
                logger.printf(Level.ERROR, " FAIL: An [ %s ] occurred creating File [ %s ]. Exiting.\n", e.getClass().getSimpleName(), path);
                e.printStackTrace(System.out);
                System.exit(1);
                return null; // lol
            }
        }

        return file;
    }

    private static void createDirs(final String forPath) {
        logger.printf(Level.INFO, "Checking directories for file [ %s ]...", forPath);

        final String[] paths = forPath.split(separator);

        if (paths.length <= 1) {
            logger.printf(Level.INFO, " OK! [ %d ]\n", paths.length);
            return;
        }

        int[] printed = new int[]{0};

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

                    printed[0]++;
                    logger.printf(Level.ERROR, " FAIL\nDirectory [ %s ] does not exist, trying to create it...\n", path);

                    if (file.mkdir()) {
                        printed[0]++;
                        logger.printf(Level.INFO, " OK!\nCreated directory [ %s ] for file [ %s ]\n", path, forPath);
                    } else {
                        printed[0]++;
                        logger.printf(Level.ERROR, " FAIL\nCould not create directory [ %s ] for file [ %s ]! Exiting.\n", path, forPath);
                        System.exit(1);
                    }
                });

        if (printed[0] == 0)
            logger.printf(Level.INFO, " OK!\n");
        else System.out.println();
    }
}
