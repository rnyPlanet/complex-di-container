package com.grin.ioc;

import com.grin.ioc.annotations.StartUp;
import com.grin.ioc.config.DIConfiguration;
import com.grin.ioc.enums.DirectoryType;
import com.grin.ioc.models.Directory;
import com.grin.ioc.models.ServiceDetails;
import com.grin.ioc.services.*;
import com.grin.ioc.services.impl.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 * Application starting point.
 *
 * Holds an instance of Dependency Container.
 */
public class Main {

    /**
     * Stores all loaded classes.
     * Only one instance of a dependency container.
     */
    public static final DependencyContainer dependencyContainer;

    static {
        dependencyContainer = new DependencyContainerImpl();
    }

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
    public static void run(Class<?> startupClass, DIConfiguration configuration) {
        ServicesScanningService scanningService = new ServicesScanningServiceImpl(configuration.annotations());

        ObjectInstantiationService objectInstantiationService = new ObjectInstantiationServiceImpl();
        InstantiationServices instantiationService = new InstantiationServicesImpl(
                configuration.instantiations(),
                objectInstantiationService
        );


        Directory directory = new DirectoryResolverImpl().resolveDirectory(startupClass);

        ClassPathScanner pathScanner = new ClassPathScannerForDirectory();
        if (directory.getDirectoryType() == DirectoryType.JAR_FILE) {
            pathScanner = new ClassPathScannerForJarFile();
        }

        Set<Class<?>> locatedClasses = pathScanner.locateClasses(directory.getDirectory());

        Set<ServiceDetails> mappedServices = scanningService.mapServices(locatedClasses);
        List<ServiceDetails> serviceDetails = instantiationService.instantiateServicesAndBeans(mappedServices);

        dependencyContainer.init(locatedClasses, serviceDetails, objectInstantiationService);
        runStartUpMethod(startupClass);
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
    private static void runStartUpMethod(Class<?> startupClass) {
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
                declaredMethod.invoke(serviceDetails.getInstance());
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }

            return;
        }
    }
}
