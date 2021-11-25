package com.grin.ioc;

import com.grin.ioc.annotations.StartUp;
import com.grin.ioc.config.DIConfiguration;
import com.grin.ioc.enums.DirectoryType;
import com.grin.ioc.models.Directory;
import com.grin.ioc.models.ServiceDetails;
import com.grin.ioc.services.*;
import com.grin.ioc.services.impl.*;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Application starting point.
 *
 * Holds an instance of Dependency Container.
 */
public class Main {

    public static void main(String[] args) {
        run(Main.class);
    }

    /**
     * Overload with default configuration.
     *
     * @param startupClass any class from the client side.
     */
    public static void run(Class<?> startupClass) {
        run(startupClass, new DIConfiguration());
    }

    /**
     * Runs with startup class.
     *
     * @param startupClass any class from the client side.
     * @param configuration client configuration.
     */
    public static DependencyContainer run(Class<?> startupClass, DIConfiguration configuration) {
        DependencyContainer dependencyContainer = run(new File[]{
                new File(new DirectoryResolverImpl().resolveDirectory(startupClass).getDirectory()),
        }, configuration);

        runStartUpMethod(startupClass, dependencyContainer);

        return dependencyContainer;
    }

    public static DependencyContainer run(File[] startupDirectories, DIConfiguration configuration) {
        ServicesScanningService scanningService = new ServicesScanningServiceImpl(configuration.annotations());

        ObjectInstantiationService objectInstantiationService = new ObjectInstantiationServiceImpl();
        InstantiationServices instantiationService = new InstantiationServicesImpl(
                configuration.instantiations(),
                objectInstantiationService
        );

        Set<Class<?>> locatedClasses = locateClasses(startupDirectories);

        Set<ServiceDetails> mappedServices = scanningService.mapServices(locatedClasses);
        List<ServiceDetails> serviceDetails = instantiationService.instantiateServicesAndBeans(mappedServices);

        DependencyContainer dependencyContainer = new DependencyContainerImpl();

        dependencyContainer.init(locatedClasses, serviceDetails, objectInstantiationService);

        return dependencyContainer;
    }

    private static Set<Class<?>> locateClasses(File[] startupDirectories) {
        Set<Class<?>> locatedClasses = new HashSet<>();
        DirectoryResolver directoryResolver = new DirectoryResolverImpl();

        for (File startupDirectory : startupDirectories) {
            final Directory directory = directoryResolver.resolveDirectory(startupDirectory);

            ClassPathScanner classLocator = new ClassPathScannerForDirectory(Thread.currentThread().getContextClassLoader());
            if (directory.getDirectoryType() == DirectoryType.JAR_FILE) {
                classLocator = new ClassPathScannerForJarFile();
            }

            locatedClasses.addAll(classLocator.locateClasses(directory.getDirectory()));
        }

        return locatedClasses;
    }

    /**
     * Method calls executes when all services are loaded.
     * <p>
     * Looks for instantiated service from the given type.
     *
     * If instance is found, looks for void method with 0 params
     * and with {@link com.grin.ioc.annotations.StartUp} annotation and executes it.
     *
     * @param startupClass any class from the client side.
     */
    private static void runStartUpMethod(Class<?> startupClass, DependencyContainer dependencyContainer) {
        ServiceDetails serviceDetails = dependencyContainer.getServiceDetails(startupClass);

        if (serviceDetails == null) {
            return;
        }

        for (Method declaredMethod : serviceDetails.getServiceType().getDeclaredMethods()) {
            if (declaredMethod.getParameterCount() != 0 ||
                    (declaredMethod.getReturnType() != void.class &&
                            declaredMethod.getReturnType() != Void.class)
                    || !declaredMethod.isAnnotationPresent(StartUp.class)) {
                continue;
            }

            declaredMethod.setAccessible(true);
            try {
                declaredMethod.invoke(serviceDetails.getActualInstance());
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }

            return;
        }
    }
}
