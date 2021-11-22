package com.grin.ioc.services;

import com.grin.ioc.exceptions.ClassLocationException;

import java.util.Set;

/**
 * Service for locating classes in the application context.
 */
public interface ClassPathScanner {

    /**
     * @param directory the given directory.
     * @return a set of located classes.
     */
    Set<Class<?>> locateClasses(String directory) throws ClassLocationException;
}
