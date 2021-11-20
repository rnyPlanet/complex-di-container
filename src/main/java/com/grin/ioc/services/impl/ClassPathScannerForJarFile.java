package com.grin.ioc.services.impl;

import com.grin.ioc.constants.Constants;
import com.grin.ioc.exceptions.ClassLocationException;
import com.grin.ioc.services.ClassPathScanner;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassPathScannerForJarFile implements ClassPathScanner {
    @Override
    public Set<Class<?>> locateClasses(String directory) throws ClassLocationException {
        Set<Class<?>> classes = new HashSet<>();

        try {
            JarFile jarFile = new JarFile(new File(directory));

            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();

                if (!jarEntry.getName().endsWith(Constants.CLASS_FILE_EXTENSION)) {
                    continue;
                }

                String className = jarEntry.getName()
                        .replace(Constants.CLASS_FILE_EXTENSION, "")
                        .replaceAll("\\\\", ".")
                        .replaceAll("/", ".");

                classes.add(Class.forName(className));
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new ClassLocationException(e.getMessage(), e);
        }

        return classes;
    }
}
