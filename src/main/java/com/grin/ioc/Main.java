package com.grin.ioc;

import com.grin.ioc.config.DIConfiguration;
import com.grin.ioc.enums.DirectoryType;
import com.grin.ioc.models.Directory;
import com.grin.ioc.services.ClassPathScanner;
import com.grin.ioc.services.impl.ClassPathScannerForDirectory;
import com.grin.ioc.services.impl.ClassPathScannerForJarFile;
import com.grin.ioc.services.impl.DirectoryResolverImpl;

import java.util.Set;

public class Main {
    public static void main(String[] args) {
        run(Main.class);
    }

    public static void run(Class<?> startupClass) {
        run(startupClass, new DIConfiguration());
    }

    public static void run(Class<?> startupClass, DIConfiguration configuration) {
        Directory directory = new DirectoryResolverImpl().resolveDirectory(startupClass);

        ClassPathScanner pathScanner = new ClassPathScannerForDirectory();
        if (directory.getDirectoryType() == DirectoryType.JAR_FILE) {
            pathScanner = new ClassPathScannerForJarFile();
        }

        Set<Class<?>> classes = pathScanner.locateClasses(directory.getDirectory());
    }

}
