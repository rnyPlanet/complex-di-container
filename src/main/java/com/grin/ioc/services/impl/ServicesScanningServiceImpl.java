package com.grin.ioc.services.impl;

import com.grin.ioc.annotations.*;
import com.grin.ioc.config.configurations.ConfigurableAnnotationsConfiguration;
import com.grin.ioc.models.ServiceDetails;
import com.grin.ioc.services.ServicesScanningService;
import com.grin.ioc.utils.ServiceDetailsConstructorComparator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
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
     * Iterates all given classes and filters those that have {@link Service} annotation
     * or one prided by the client and collects details for those classes.
     *
     * @param locatedClasses given set of classes.
     * @return set or services and their collected details.
     */
    @Override
    public Set<ServiceDetails> mapServices(Set<Class<?>> locatedClasses) {
        Set<ServiceDetails> serviceDetails = new HashSet<>();
        Set<Class<? extends Annotation>> serviceAnnotations = configuration.getServiceAnnotations();

        for (Class<?> cls : locatedClasses) {
            if (cls.isInterface()) {
                continue;
            }

            for (Annotation annotation : cls.getAnnotations()) {
                if (serviceAnnotations.contains(annotation.annotationType())) {
                    ServiceDetails detailsService = new ServiceDetails(
                            cls,
                            annotation,
                            this.findSuitableConstructor(cls),
                            this.findVoidMethodWithZeroParamsAndAnnotation(PostConstruct.class, cls),
                            this.findVoidMethodWithZeroParamsAndAnnotation(PreDestroy.class, cls),
                            this.findBeans(cls)
                            );

                    serviceDetails.add(detailsService);

                    break;
                }
            }
        }

        return serviceDetails.stream()
                .sorted(new ServiceDetailsConstructorComparator())
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
        }

        return cls.getConstructors()[0];
    }

    private Method findVoidMethodWithZeroParamsAndAnnotation(Class<? extends Annotation> annotation,
                                                             Class<?> cls) {
        for (Method method : cls.getDeclaredMethods()) {
            if (method.getParameterCount() != 0 ||
                    (method.getReturnType() != void.class && method.getReturnType() != Void.class) ||
                    !method.isAnnotationPresent(annotation)) {
                continue;
            }

            method.setAccessible(true);
            return method;
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
