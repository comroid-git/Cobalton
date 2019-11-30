package de.comroid.util.files;

import java.io.File;
import java.io.IOException;

public class FileProvider {
    private final static String PREFIX = "./";

    public static File getFile(String subPath) {
        final String path = PREFIX + subPath;
        final File file = new File(path);

        if (!file.exists()) {
            System.out.printf("File [ %s ] does not exist. Trying to create it...\n", path);

            try {
                if (file.createNewFile()) {
                    System.out.printf("Could not create File [ %s ]. Exiting.\n", path);
                    System.exit(1);
                    return null; // lol
                }
            } catch (IOException e) {
                System.out.printf(String.format("An [ %s ] occurred creating File [ %s ]. Exiting.\n", e.getClass().getSimpleName(), path), path);
                e.printStackTrace(System.out);
                System.exit(1);
                return null; // lol
            }
        }

        return file;
    }
}
