package com.grin.ioc.services.impl;

import com.grin.ioc.config.configurations.InstantiationConfiguration;
import com.grin.ioc.exceptions.ServiceInstantiationException;
import com.grin.ioc.models.EnqueuedServiceDetails;
import com.grin.ioc.models.ServiceBeanDetails;
import com.grin.ioc.models.ServiceDetails;
import com.grin.ioc.services.InstantiationServices;
import com.grin.ioc.services.ObjectInstantiationService;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class InstantiationServicesImpl implements InstantiationServices {

    private static final String MAX_NUMBER_OF_ALLOWED_ITERATIONS_REACHED = "Maximum number of allowed iterations was reached '%s'.";
    private static final String COULD_NOT_FIND_CONSTRUCTOR_PARAM_MSG = "Could not create instance of '%s'. Parameter '%s' implementation was not found";

    private InstantiationConfiguration configuration;
    private ObjectInstantiationService instantiationService;
    private LinkedList<EnqueuedServiceDetails> enqueuedServiceDetails;
    private List<Class<?>> allAvailableClasses;
    private List<ServiceDetails<?>> instantiatedServices;

    public InstantiationServicesImpl(InstantiationConfiguration configuration,
                                     ObjectInstantiationService instantiationService) {
        this.configuration = configuration;
        this.instantiationService = instantiationService;
        this.enqueuedServiceDetails = new LinkedList<>();
        this.allAvailableClasses = new ArrayList<>();
        this.instantiatedServices = new ArrayList<>();
    }

    @Override
    public List<ServiceDetails<?>> instantiateServicesAndBeans(Set<ServiceDetails<?>> mappedServices) throws ServiceInstantiationException {
        this.init(mappedServices);
        this.checkForMissingServices(mappedServices);

        int counter = 0;
        int maxNumberIteration = this.configuration.getMaxNumberIteration();

        while (!this.enqueuedServiceDetails.isEmpty()) {
            if (counter > maxNumberIteration) {
                throw new ServiceInstantiationException(String.format(MAX_NUMBER_OF_ALLOWED_ITERATIONS_REACHED, maxNumberIteration));
            }

            EnqueuedServiceDetails enqueuedServiceDetails = this.enqueuedServiceDetails.removeFirst();

            if (enqueuedServiceDetails.isResolved()) {
                ServiceDetails<?> serviceDetails = enqueuedServiceDetails.getServiceDetails();
                Object[] dependencyInstances = enqueuedServiceDetails.getDependencyInstances();

                this.instantiationService.createInstance(serviceDetails, dependencyInstances);
                this.registerInstantiatedService(serviceDetails);
                this.registerBeans(serviceDetails);
            } else {
                this.enqueuedServiceDetails.addLast(enqueuedServiceDetails);
                counter++;
            }
        }

        return this.instantiatedServices;
    }

    private void registerInstantiatedService(ServiceDetails<?> serviceDetails) {
        if (!(serviceDetails instanceof ServiceBeanDetails)) {
            this.updatedDependantServices(serviceDetails);
        }

        this.instantiatedServices.add(serviceDetails);

        for (EnqueuedServiceDetails enqueuedService : this.enqueuedServiceDetails) {
            if (enqueuedService.isDependencyRequired(serviceDetails.getServiceType())) {
                enqueuedService.addDependencyInstance(serviceDetails.getInstance());
            }
        }
    }

    private void updatedDependantServices(ServiceDetails<?> newService) {
        for (Class<?> parameterType : newService.getTargetConstructor().getParameterTypes()) {
            for (ServiceDetails<?> serviceDetails : this.instantiatedServices) {
                if (parameterType.isAssignableFrom(serviceDetails.getServiceType())) {
                    serviceDetails.addDependentServices(newService);
                }
            }
        }
    }

    private void registerBeans(ServiceDetails<?> serviceDetails) {
        for (Method beanMethod : serviceDetails.getBeans()) {
            ServiceBeanDetails<?> beanDetails = new ServiceBeanDetails<>(beanMethod.getReturnType(), beanMethod, serviceDetails);
            this.instantiationService.createBeanInstance(beanDetails);
            this.registerInstantiatedService(beanDetails);
        }
    }

    private void checkForMissingServices(Set<ServiceDetails<?>> mappedServices) throws ServiceInstantiationException {
        for (ServiceDetails<?> serviceDetails : mappedServices) {
            for (Class<?> parameterType : serviceDetails.getTargetConstructor().getParameterTypes()) {
                if (!this.isAssignableTypePresent(parameterType)) {
                    throw new ServiceInstantiationException(
                            String.format(COULD_NOT_FIND_CONSTRUCTOR_PARAM_MSG,
                                    serviceDetails.getServiceType().getName(),
                                    parameterType.getName()
                            )
                    );
                }
            }
        }
    }

    private boolean isAssignableTypePresent(Class<?> cls) {
        for (Class<?> serviceType : this.allAvailableClasses) {
            if (cls.isAssignableFrom(serviceType)) {
                return true;
            }
        }

        return false;
    }

    private void init(Set<ServiceDetails<?>> mappedServices) {
        this.enqueuedServiceDetails.clear();
        this.allAvailableClasses.clear();
        this.instantiatedServices.clear();

        for (ServiceDetails<?> serviceDetails : mappedServices) {
            this.enqueuedServiceDetails.add(new EnqueuedServiceDetails(serviceDetails));
            this.allAvailableClasses.add(serviceDetails.getServiceType());
            this.allAvailableClasses.addAll(
                    Arrays.stream(serviceDetails.getBeans())
                            .map(Method::getReturnType)
                            .collect(Collectors.toList())
            );
        }
    }
}