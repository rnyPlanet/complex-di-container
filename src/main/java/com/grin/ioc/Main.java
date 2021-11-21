package com.grin.ioc;

import com.grin.ioc.annotations.Service;
import com.grin.ioc.config.DIConfiguration;
import com.grin.ioc.enums.DirectoryType;
import com.grin.ioc.models.Directory;
import com.grin.ioc.models.ServiceDetails;
import com.grin.ioc.services.ClassPathScanner;
import com.grin.ioc.services.InstantiationServices;
import com.grin.ioc.services.ObjectInstantiationService;
import com.grin.ioc.services.ServicesScanningService;
import com.grin.ioc.services.impl.*;

import java.util.List;
import java.util.Set;

@Service
public class Main {

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

        Set<ServiceDetails<?>> mappedServices = scanningService.mapServices(locatedClasses);
        List<ServiceDetails<?>> serviceDetails = instantiationService.instantiateServicesAndBeans(mappedServices);
    }

}
