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

public class Main {

    public static final DependencyContainer dependencyContainer;

    static {
        dependencyContainer = new DependencyContainerImpl();
    }

    public static void main(String[] args) {
        run(Main.class);
    }

    public static void run(Class<?> startupClass) {
        run(startupClass, new DIConfiguration());
    }

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

        dependencyContainer.init(serviceDetails, objectInstantiationService);
        runStartUpMethod(startupClass);
    }

    private static void runStartUpMethod(Class<?> startupClass) {
        ServiceDetails serviceDetails = dependencyContainer.getServiceDetails(startupClass);

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
