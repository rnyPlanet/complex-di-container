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

public class ServicesScanningServiceImpl implements ServicesScanningService {

    private ConfigurableAnnotationsConfiguration configuration;

    public ServicesScanningServiceImpl(ConfigurableAnnotationsConfiguration configuration) {
        this.configuration = configuration;
        this.init();
    }

    private void init() {
        this.configuration.getBeanAnnotations().add(Bean.class);
        this.configuration.getServiceAnnotations().add(Service.class);
    }
    
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
