package com.grin.ioc.services.impl;

import com.grin.ioc.annotations.*;
import com.grin.ioc.config.configurations.ConfigurableAnnotationsConfiguration;
import com.grin.ioc.models.ServiceDetails;
import com.grin.ioc.services.ServicesScanningService;
import com.grin.ioc.utils.AliasFinder;
import com.grin.ioc.utils.ServiceDetailsConstructorComparator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@link ServicesScanningService} implementation.
 *
 * Iterates all located classes and looks for classes with {@link @Service}
 * annotation or one provided by the client and then collects data for that class.
 */
public class ServicesScanningServiceImpl implements ServicesScanningService {

    /**
     * Configuration containing annotations provided by the client.
     */
    private ConfigurableAnnotationsConfiguration configuration;

    public ServicesScanningServiceImpl(ConfigurableAnnotationsConfiguration configuration) {
        this.configuration = configuration;
        this.init();
    }

    /**
     * Adds the platform's default annotations for services and beans on top of the
     * ones that the client might have provided.
     */
    private void init() {
        this.configuration.getBeanAnnotations().add(Bean.class);
        this.configuration.getServiceAnnotations().add(Service.class);
    }

    /**
     * Iterates scanned classes with @{@link Service} or user specified annotation
     * and creates a {@link ServiceDetails} object with the collected information.
     *
     * @param locatedClasses given set of classes.
     * @return set or services and their collected details.
     */
    @Override
    public Set<ServiceDetails> mapServices(Set<Class<?>> locatedClasses) {
        Map<Class<?>, Annotation> onlyServiceClasses = this.filterServiceClasses(locatedClasses);

        Set<ServiceDetails> serviceDetailsStorage = new HashSet<>();

        for (Map.Entry<Class<?>, Annotation> serviceAnnotationEntry : onlyServiceClasses.entrySet()) {
            Class<?> cls = serviceAnnotationEntry.getKey();
            Annotation annotation = serviceAnnotationEntry.getValue();

            ServiceDetails serviceDetails = new ServiceDetails(
                    cls,
                    annotation,
                    this.findSuitableConstructor(cls),
                    this.findVoidMethodWithZeroParamsAndAnnotation(PostConstruct.class, cls),
                    this.findVoidMethodWithZeroParamsAndAnnotation(PreDestroy.class, cls),
                    this.findBeans(cls)
            );

            serviceDetailsStorage.add(serviceDetails);

        }

        return serviceDetailsStorage.stream()
                .sorted(new ServiceDetailsConstructorComparator())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Iterates all given classes and filters those that have {@link Service} annotation
     * or one prided by the client.
     *
     * @return service annotated classes.
     */
    private Map<Class<?>, Annotation> filterServiceClasses(Collection<Class<?>> scannedClasses) {
        Set<Class<? extends Annotation>> serviceAnnotations = this.configuration.getServiceAnnotations();
        Map<Class<?>, Annotation> locatedClasses = new HashMap<>();

        for (Class<?> cls : scannedClasses) {
            if (cls.isInterface() || cls.isEnum() || cls.isAnnotation()) {
                continue;
            }

            for (Annotation annotation : cls.getAnnotations()) {
                if (serviceAnnotations.contains(annotation.annotationType())) {
                    locatedClasses.put(cls, annotation);
                    break;
                }
            }
        }

        this.configuration.getAdditionalClasses().forEach((cls, a) -> {
            Annotation annotation = null;
            if (a != null && cls.isAnnotationPresent(a)) {
                annotation = cls.getAnnotation(a);
            }

            locatedClasses.put(cls, annotation);
        });

        return locatedClasses;
    }

    /**
     * Looks for a constructor from the given class that has {@link Autowired} annotation
     * or gets the first one.
     *
     * @param cls - the given class.
     * @return suitable constructor.
     */
    private Constructor<?> findSuitableConstructor(Class<?> cls) {
        for (Constructor<?> ctr : cls.getDeclaredConstructors()) {
            if (ctr.isAnnotationPresent(Autowired.class)) {
                ctr.setAccessible(true);
                return ctr;
            }

            for (Annotation declaredAnnotation : ctr.getDeclaredAnnotations()) {
                final Class<? extends Annotation> aliasAnnotation = AliasFinder.getAliasAnnotation(declaredAnnotation, Autowired.class);
                if (aliasAnnotation != null) {
                    ctr.setAccessible(true);
                    return ctr;
                }
            }
        }

        return cls.getConstructors()[0];
    }

    private Method findVoidMethodWithZeroParamsAndAnnotation(Class<? extends Annotation> annotation,
                                                             Class<?> cls) {
        for (Method method : cls.getDeclaredMethods()) {
            if (method.getParameterCount() != 0 ||
                    (method.getReturnType() != void.class && method.getReturnType() != Void.class)) {
                continue;
            }

            if (method.isAnnotationPresent(annotation)) {
                method.setAccessible(true);
                return method;
            }

            for (Annotation declaredAnnotation : method.getDeclaredAnnotations()) {
                Class<? extends Annotation> aliasAnnotation = AliasFinder.getAliasAnnotation(declaredAnnotation, annotation);

                if (aliasAnnotation != null) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }

        if (cls.getSuperclass() != null) {
            return this.findVoidMethodWithZeroParamsAndAnnotation(annotation, cls.getSuperclass());
        }

        return null;
    }

    /**
     * Scans a given class for methods that are considered beans.
     *
     * @param cls the given class.
     * @return array or method references that are bean compliant.
     */
    private Method[] findBeans(Class<?> cls) {
        Set<Method> beanMethods = new HashSet<>();

        for (Method method : cls.getDeclaredMethods()) {
            if (method.getParameterCount() != 0 ||  method.getReturnType() == void.class || method.getReturnType() == Void.class) {
                continue;
            }

            for (Class<? extends Annotation> beanAnnotation : this.configuration.getBeanAnnotations()) {
                if (method.isAnnotationPresent(beanAnnotation)) {
                    method.setAccessible(true);
                    beanMethods.add(method);

                    break;
                }
            }
        }

        return beanMethods.toArray(Method[]::new);
    }
}
