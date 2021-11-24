package com.grin.ioc.services.impl;

import com.grin.ioc.exceptions.AlreadyInitializedException;
import com.grin.ioc.models.ServiceBeanDetails;
import com.grin.ioc.models.ServiceDetails;
import com.grin.ioc.services.DependencyContainer;
import com.grin.ioc.services.ObjectInstantiationService;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Container for all services and beans.
 *
 * <p>
 * Contains functionality for managing the application context
 * by reloading or accessing certain services.
 */
public class DependencyContainerImpl implements DependencyContainer {

    private static final String ALREADY_INITIALIZED_MSG = "Dependency container already initialized.";
    private static final String SERVICE_NOT_FOUND_FORMAT = "Service \"%s\" was not found.";

    private boolean isInit;
    private Collection<Class<?>> allLocatedClasses;
    private Collection<ServiceDetails> servicesAndBeans;
    private ObjectInstantiationService instantiationService;

    private Map<Class<?>, ServiceDetails> cachedServices;
    private Map<Class<?>, Collection<ServiceDetails>> cachedImplementations;
    private Map<Class<? extends Annotation>, Collection<ServiceDetails>> cachedServicesByAnnotation;

    public DependencyContainerImpl() {
        this.isInit = false;

        this.cachedServices = new HashMap<>();
        this.cachedImplementations = new HashMap<>();
        this.cachedServicesByAnnotation = new HashMap<>();
    }

    @Override
    public void init(Collection<Class<?>> locatedClasses, Collection<ServiceDetails> servicesAndBeans, ObjectInstantiationService instantiationService) throws AlreadyInitializedException {
        if (this.isInit) {
            throw new AlreadyInitializedException(ALREADY_INITIALIZED_MSG);
        }

        this.allLocatedClasses = locatedClasses;
        this.servicesAndBeans = servicesAndBeans;
        this.instantiationService = instantiationService;

        this.isInit = true;
    }

    /**
     * Creates a new instance for a given service and destroys the current one.
     *
     * @param serviceDetails - the given service.
     */
    @Override
    public void reload(ServiceDetails serviceDetails) {
        this.instantiationService.destroyInstance(serviceDetails);
        this.handleReload(serviceDetails);
    }

    @Override
    public void reload(Class<?> serviceType) {
        final ServiceDetails serviceDetails = this.getServiceDetails(serviceType);
        if (serviceDetails == null) {
            throw new IllegalArgumentException(String.format(SERVICE_NOT_FOUND_FORMAT, serviceType));
        }

        this.reload(serviceDetails);
    }

    @Override
    public void update(Object service) {
        ServiceDetails serviceDetails = this.getServiceDetails(service.getClass());
        if (serviceDetails == null) {
            throw new IllegalArgumentException(String.format(SERVICE_NOT_FOUND_FORMAT, service));
        }

        this.instantiationService.destroyInstance(serviceDetails);
        serviceDetails.setInstance(service);
    }

    /**
     * Handles different types of service.
     *
     * <p>
     * If the service is bean, it does not have a constructor, but an origin method.
     *
     * @param serviceDetails - target service.
     */
    private void handleReload(ServiceDetails serviceDetails) {
        if (serviceDetails instanceof ServiceBeanDetails) {
            this.instantiationService.createBeanInstance((ServiceBeanDetails) serviceDetails);

            //Since beans are not proxies, reload all dependant classes.
            for (ServiceDetails dependantService : serviceDetails.getDependentServices()) {
                this.reload(dependantService);
            }
        } else {
            this.instantiationService.createInstance(serviceDetails, this.collectDependencies(serviceDetails));
        }
    }

    /**
     * Gets instances of all required dependencies for a given service.
     *
     * @param serviceDetails - the given service.
     * @return array of instantiated dependencies.
     */
    private Object[] collectDependencies(ServiceDetails serviceDetails) {
        Class<?>[] parameterTypes = serviceDetails.getTargetConstructor().getParameterTypes();
        Object[] dependencyInstances = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            dependencyInstances[i] = this.getService(parameterTypes[i]);
        }

        return dependencyInstances;
    }

    /**
     * Gets service instance for a given type.
     *
     * @param serviceType the given type.
     * @param <T>         generic type.
     * @return instance of the required service or null.
     */
    @Override
    public <T> T getService(Class<T> serviceType) {
        ServiceDetails serviceDetails = this.getServiceDetails(serviceType);

        if (serviceDetails != null) {
            return (T) serviceDetails.getProxyInstance();
        }

        return null;
    }

    /**
     * Gets service details object for a given service type.
     *
     * @param serviceType - the given service type.
     * @return service details or null.
     */
    @Override
    public ServiceDetails getServiceDetails(Class<?> serviceType) {
        if (this.cachedServices.containsKey(serviceType)) {
            return this.cachedServices.get(serviceType);
        }

        ServiceDetails serviceDetails = this.servicesAndBeans.stream()
                .filter(sd -> serviceType.isAssignableFrom(sd.getProxyInstance().getClass()) || serviceType.isAssignableFrom(sd.getServiceType()))
                .findFirst().orElse(null);

        if (serviceDetails != null) {
            this.cachedServices.put(serviceType, serviceDetails);
        }

        return serviceDetails;
    }

    /**
     * @return a collection of all classes that were found in the application
     * including even classes that are not annotated with any annotation.
     */
    @Override
    public Collection<Class<?>> getAllScannedClasses() {
        return this.allLocatedClasses;
    }

    /**
     * @param serviceType given interface.
     * @return collection of service details that implement the given interface.
     */
    @Override
    public Collection<ServiceDetails> getImplementations(Class<?> serviceType) {
        if (this.cachedImplementations.containsKey(serviceType)) {
            return this.cachedImplementations.get(serviceType);
        }

        List<ServiceDetails> implementations = this.servicesAndBeans.stream()
                .filter(sd -> serviceType.isAssignableFrom(sd.getServiceType()))
                .collect(Collectors.toList());

        this.cachedImplementations.put(serviceType, implementations);

        return implementations;
    }

    /**
     * Gets all services that are mapped with a given annotation.
     *
     * @param annotationType the given annotation.
     */
    @Override
    public Collection<ServiceDetails> getServicesByAnnotation(Class<? extends Annotation> annotationType) {
        if (this.cachedServicesByAnnotation.containsKey(annotationType)) {
            return this.cachedServicesByAnnotation.get(annotationType);
        }

        List<ServiceDetails> serviceDetailsByAnnotation = this.servicesAndBeans.stream()
                .filter(sd -> sd.getAnnotation() != null && sd.getAnnotation().annotationType() == annotationType)
                .collect(Collectors.toList());

        this.cachedServicesByAnnotation.put(annotationType, serviceDetailsByAnnotation);

        return serviceDetailsByAnnotation;
    }

    /**
     * Gets only the instances of all services.
     */
    @Override
    public Collection<ServiceDetails> getAllServices() {
        return this.servicesAndBeans;
    }
}
