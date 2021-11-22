package com.grin.ioc.services.impl;

import com.grin.ioc.constants.Constants;
import com.grin.ioc.enums.DirectoryType;
import com.grin.ioc.models.Directory;
import com.grin.ioc.services.DirectoryResolver;

import java.io.File;

/**
 * DirectoryResolver implementation.
 * Determines the type of the directory, from which the application
 * is started.
 */
public class DirectoryResolverImpl implements DirectoryResolver {

    @Override
    public Directory resolveDirectory(Class<?> startupClass) {
        String directory = this.getDirectory(startupClass);

        return new Directory(directory, this.getDirectoryType(directory));
    }

    /**
     * Get the root dir where the given class resides.
     *
     * @param cls - the given class.
     */
    private String getDirectory(Class<?> cls) {
        return cls.getProtectionDomain().getCodeSource().getLocation().getFile();
    }

    /**
     * @param directory given directory.
     * @return JAR_FILE or DIRECTORY.
     */
    private DirectoryType getDirectoryType(String directory) {
        File file = new File(directory);

        if (!file.isDirectory() && directory.endsWith(Constants.JAR_FILE_EXTENSION)) {
            return DirectoryType.JAR_FILE;
        }

        return DirectoryType.DIRECTORY;
    }
}
