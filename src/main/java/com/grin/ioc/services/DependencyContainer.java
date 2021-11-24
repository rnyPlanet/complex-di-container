package com.grin.ioc.services;

import com.grin.ioc.exceptions.AlreadyInitializedException;
import com.grin.ioc.models.ServiceDetails;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

public interface DependencyContainer {
    void init(Collection<Class<?>> locatedClasses, Collection<ServiceDetails> servicesAndBeans, ObjectInstantiationService instantiationService) throws AlreadyInitializedException;

    void reload(ServiceDetails serviceDetails);

    void reload(Class<?> serviceType);

    void update(Object service);

    <T> T getService(Class<T> serviceType);

    ServiceDetails getServiceDetails(Class<?> serviceType);

    Collection<Class<?>> getAllScannedClasses();

    Collection<ServiceDetails> getImplementations(Class<?> serviceType);

    Collection<ServiceDetails> getServicesByAnnotation(Class<? extends Annotation> annotationType);

    Collection<ServiceDetails> getAllServices();
}
