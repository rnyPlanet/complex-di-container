package com.grin.ioc.models;

import com.grin.ioc.enums.DirectoryType;

/**
 * Simple POJO class that stores the directory to the source code/jar and the directory type.
 */
public class Directory {

    /**
     * Stores actual directory value.
     */
    private String directory;

    /**
     * Stores the type of the directory (Directory or Jar file).
     */
    private DirectoryType directoryType;

    public Directory(String directory, DirectoryType directoryType) {
        this.directory = directory;
        this.directoryType = directoryType;
    }

    public String getDirectory() {
        return directory;
    }

    public DirectoryType getDirectoryType() {
        return directoryType;
    }
}
