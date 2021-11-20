package com.grin.ioc.models;

import com.grin.ioc.enums.DirectoryType;

public class Directory {
    private String directory;
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
