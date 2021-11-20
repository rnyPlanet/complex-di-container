package com.grin.ioc.services.impl;

import com.grin.ioc.constants.Constants;
import com.grin.ioc.exceptions.ClassLocationException;
import com.grin.ioc.services.ClassPathScanner;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class ClassPathScannerForDirectory implements ClassPathScanner {
    private static final String INVALID_DIRECTORY_MSG = "Invalid directory '%s'.";

    private final Set<Class<?>> locatedClasses;

    public ClassPathScannerForDirectory() {
        this.locatedClasses = new HashSet<>();
    }

    @Override
    public Set<Class<?>> locateClasses(String directory) throws ClassLocationException {
        this.locatedClasses.clear();

        File file = new File(directory);

        if (!file.isDirectory()) {
            throw new ClassLocationException(String.format(INVALID_DIRECTORY_MSG, directory));
        }

        try {
            for (File innerFile : file.listFiles()) {
                this.scanDir(innerFile, "");
            }
        } catch (ClassNotFoundException e) {
            throw new ClassLocationException(e.getMessage(), e);
        }

        return this.locatedClasses;
    }

    private void scanDir(File file, String packageName) throws ClassNotFoundException {
        if (file.isDirectory()) {
            packageName += file.getName() + ".";

            for (File innerFile : file.listFiles()) {
                this.scanDir(innerFile, packageName);
            }
        } else {
            if (!file.getName().endsWith(Constants.CLASS_FILE_EXTENSION)) {
                return;
            }

            String className = packageName + file.getName()
                    .replace(Constants.CLASS_FILE_EXTENSION, "");

            this.locatedClasses.add(Class.forName(className));
        }
    }

}
