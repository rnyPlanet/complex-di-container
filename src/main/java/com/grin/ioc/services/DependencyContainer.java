package com.grin.ioc.services;

import com.grin.ioc.exceptions.AlreadyInitializedException;
import com.grin.ioc.models.ServiceDetails;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

public interface DependencyContainer {
    void init(Collection<Class<?>> locatedClasses, List<ServiceDetails> servicesAndBeans, ObjectInstantiationService instantiationService) throws AlreadyInitializedException;

    void reload(ServiceDetails serviceDetails, boolean reloadDependentServices);

    <T> T reload(T service);

    <T> T reload(T service, boolean reloadDependentServices);

    <T> T getService(Class<T> serviceType);

    ServiceDetails getServiceDetails(Class<?> serviceType);

    List<ServiceDetails> getServicesByAnnotation(Class<? extends Annotation> annotationType);

    List<Object> getAllServices();

    List<ServiceDetails> getAllServiceDetails();

    Collection<Class<?>> getLocatedClasses();
}
