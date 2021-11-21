package com.grin.ioc.services;

import com.grin.ioc.models.Directory;

public interface DirectoryResolver {
    Directory resolveDirectory(Class<?> startupClass);
}
