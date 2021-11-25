package com.grin.ioc.services;

import com.grin.ioc.models.Directory;

import java.io.File;

public interface DirectoryResolver {
    Directory resolveDirectory(Class<?> startupClass);

    Directory resolveDirectory(File directory);
}
