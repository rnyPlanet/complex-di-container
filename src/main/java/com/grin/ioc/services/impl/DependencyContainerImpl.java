package com.grin.ioc.services.impl;

import com.grin.ioc.exceptions.AlreadyInitializedException;
import com.grin.ioc.models.ServiceBeanDetails;
import com.grin.ioc.models.ServiceDetails;
import com.grin.ioc.services.DependencyContainer;
import com.grin.ioc.services.ObjectInstantiationService;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DependencyContainerImpl implements DependencyContainer {

    private static final String ALREADY_INITIALIZED_MSG = "Dependency container already initialized.";

    private boolean isInit;
    private List<ServiceDetails> servicesAndBeans;
    private ObjectInstantiationService instantiationService;

    public DependencyContainerImpl() {
        this.isInit = false;
    }

    @Override
    public void init(List<ServiceDetails> servicesAndBeans, ObjectInstantiationService instantiationService) throws AlreadyInitializedException {
        if (this.isInit) {
            throw new AlreadyInitializedException(ALREADY_INITIALIZED_MSG);
        }

        this.servicesAndBeans = servicesAndBeans;
        this.instantiationService = instantiationService;

        this.isInit = true;
    }

    @Override
    public <T> void reload(ServiceDetails serviceDetails, boolean reloadDependentServices) {
        this.instantiationService.destroyInstance(serviceDetails);
        this.handleReload(serviceDetails);

        if (reloadDependentServices) {
            for (ServiceDetails dependantService : serviceDetails.getDependentServices()) {
                this.reload(dependantService, reloadDependentServices);
            }
        }
    }

    private void handleReload(ServiceDetails serviceDetails) {
        if (serviceDetails instanceof ServiceBeanDetails) {
            this.instantiationService.createBeanInstance((ServiceBeanDetails) serviceDetails);
        } else {
            this.instantiationService.createInstance(serviceDetails, this.collectDependencies(serviceDetails));
        }
    }

    private Object[] collectDependencies(ServiceDetails serviceDetails) {
        Class<?>[] parameterTypes = serviceDetails.getTargetConstructor().getParameterTypes();
        Object[] dependencyInstances = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            dependencyInstances[i] = this.getService(parameterTypes[i]);
        }

        return dependencyInstances;
    }

    @Override
    public <T> T reload(T service) {
        return this.reload(service, false);
    }

    @Override
    public <T> T reload(T service, boolean reloadDependentServices) {
        ServiceDetails serviceDetails = this.getServiceDetails(service.getClass());

        if (serviceDetails == null) {
            return null;
        }

        this.reload(serviceDetails, reloadDependentServices);

        return (T) serviceDetails.getInstance();
    }

    @Override
    public <T> T getService(Class<T> serviceType) {
        ServiceDetails serviceDetails = this.getServiceDetails(serviceType);

        if (serviceDetails != null) {
            return (T) serviceDetails.getInstance();
        }

        return null;
    }

    @Override
    public <T> ServiceDetails getServiceDetails(Class<T> serviceType) {
        return this.servicesAndBeans.stream()
                .filter(sd -> serviceType.isAssignableFrom(sd.getServiceType()))
                .findFirst().orElse(null);
    }

    @Override
    public List<ServiceDetails> getServicesByAnnotation(Class<? extends Annotation> annotationType) {
        return this.servicesAndBeans.stream()
                .filter(sd -> sd.getAnnotation().annotationType() == annotationType)
                .collect(Collectors.toList());
    }

    @Override
    public List<Object> getAllServices() {
        return this.servicesAndBeans.stream()
                .map(ServiceDetails::getInstance)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceDetails> getAllServiceDetails() {
        return Collections.unmodifiableList(this.servicesAndBeans);
    }
}
