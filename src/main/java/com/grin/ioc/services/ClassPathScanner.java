package com.grin.ioc.services;

import com.grin.ioc.exceptions.ClassLocationException;

import java.util.Set;

public interface ClassPathScanner {
    Set<Class<?>> locateClasses(String directory) throws ClassLocationException;
}
