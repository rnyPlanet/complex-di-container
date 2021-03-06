package com.grin.ioc.services.impl;

import com.grin.ioc.annotations.Nullable;
import com.grin.ioc.config.configurations.InstantiationConfiguration;
import com.grin.ioc.exceptions.ServiceInstantiationException;
import com.grin.ioc.models.EnqueuedServiceDetails;
import com.grin.ioc.models.ServiceBeanDetails;
import com.grin.ioc.models.ServiceDetails;
import com.grin.ioc.services.InstantiationServices;
import com.grin.ioc.services.ObjectInstantiationService;
import com.grin.ioc.utils.AliasFinder;
import com.grin.ioc.utils.ProxyUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@link InstantiationServices} implementation.
 *
 * <p>
 * Responsible for creating the initial instances or all services and beans.
 */
public class InstantiationServicesImpl implements InstantiationServices {

    private static final String MAX_NUMBER_OF_ALLOWED_ITERATIONS_REACHED = "Maximum number of allowed iterations was reached '%s'. Remaining services: \n %s";
    private static final String COULD_NOT_FIND_CONSTRUCTOR_PARAM_MSG = "Could not create instance of '%s'. Parameter '%s' implementation was not found";
    private static final String COULD_NOT_FIND_FIELD_PARAM_MSG = "Could not create instance of '%s'. Implementation was not found for Autowired field '%s'.";

    /**
     * Configuration containing the maximum number or allowed iterations.
     */
    private InstantiationConfiguration configuration;

    private ObjectInstantiationService instantiationService;

    /**
     * The storage for all services that are waiting to the instantiated.
     */
    private LinkedList<EnqueuedServiceDetails> enqueuedServiceDetails;

    /**
     * Contains all available types that will be loaded from this service.
     * This includes all services and all beans.
     */
    private List<Class<?>> allAvailableClasses;

    /**
     * Contains services and beans that have been loaded.
     */
    private List<ServiceDetails> instantiatedServices;

    public InstantiationServicesImpl(InstantiationConfiguration configuration,
                                     ObjectInstantiationService instantiationService) {
        this.configuration = configuration;
        this.instantiationService = instantiationService;
        this.enqueuedServiceDetails = new LinkedList<>();
        this.allAvailableClasses = new ArrayList<>();
        this.instantiatedServices = new ArrayList<>();
    }

    /**
     * Starts looping and for each cycle gets the first element in the enqueuedServiceDetails since they are ordered
     * by the number of constructor params in ASC order.
     *
     * <p>
     * If the {@link EnqueuedServiceDetails} is resolved (all of its dependencies have instances or there are no params)
     * then the service is being instantiated, registered and its beans are also being registered.
     * Otherwise the element is added back to the enqueuedServiceDetails at the last position.
     *
     * @param mappedServices provided services and their details.
     * @return list of all instantiated services and beans.
     * @throws ServiceInstantiationException if maximum number of iterations is reached
     *                                       One iteration is added only of a service has not been instantiated in this cycle.
     */
    @Override
    public List<ServiceDetails> instantiateServicesAndBeans(Set<ServiceDetails> mappedServices) throws ServiceInstantiationException {
        this.init(mappedServices);

        int counter = 0;
        int maxNumberIteration = this.configuration.getMaxNumberIteration();

        while (!this.enqueuedServiceDetails.isEmpty()) {
            if (counter > maxNumberIteration) {
                throw new ServiceInstantiationException(String.format(
                        MAX_NUMBER_OF_ALLOWED_ITERATIONS_REACHED,
                        maxNumberIteration,
                        this.enqueuedServiceDetails)
                );
            }

            EnqueuedServiceDetails enqueuedServiceDetails = this.enqueuedServiceDetails.removeFirst();

            if (enqueuedServiceDetails.isResolved()) {
                ServiceDetails serviceDetails = enqueuedServiceDetails.getServiceDetails();
                Object[] dependencyInstances = enqueuedServiceDetails.getDependencyInstances();

                this.instantiationService.createInstance(serviceDetails, dependencyInstances, enqueuedServiceDetails.getFieldDependencyInstances());
                ProxyUtils.createProxyInstance(serviceDetails, enqueuedServiceDetails.getDependencyInstances());

                this.registerInstantiatedService(serviceDetails);
                this.registerBeans(serviceDetails);
            } else {
                this.enqueuedServiceDetails.addLast(enqueuedServiceDetails);
                counter++;
            }
        }

        return this.instantiatedServices;
    }

    /**
     * Adds the newly created service to the list of instantiated services instantiatedServices.
     * Iterated all enqueued services and if one of them relies on that services, adds its instance
     * to them so they can get resolved.
     *
     * @param newlyCreatedService - the created service.
     */
    private void registerInstantiatedService(ServiceDetails newlyCreatedService) {
        if (!(newlyCreatedService instanceof ServiceBeanDetails)) {
            this.updatedDependantServices(newlyCreatedService);
        }

        this.instantiatedServices.add(newlyCreatedService);

        for (EnqueuedServiceDetails enqueuedService : this.enqueuedServiceDetails) {
            if (enqueuedService.isDependencyRequired(newlyCreatedService.getServiceType())) {
                enqueuedService.addDependencyInstance(newlyCreatedService.getActualInstance());
            }
        }
    }

    /**
     * Gets all dependencies of the given new service.
     *
     * <p>
     * For each dependency, in the form of {@link ServiceDetails}
     * adds itself to its dependant services list.
     *
     * @param newService - the newly created service.
     */
    private void updatedDependantServices(ServiceDetails newService) {
        for (Class<?> parameterType : newService.getTargetConstructor().getParameterTypes()) {
            for (ServiceDetails serviceDetails : this.instantiatedServices) {
                if (parameterType.isAssignableFrom(serviceDetails.getServiceType())) {
                    serviceDetails.addDependentServices(newService);
                }
            }
        }
    }

    /**
     * Iterates all bean methods for the given service.
     * Creates {@link ServiceBeanDetails} and then creates instance of the bean.
     * Finally calls registerInstantiatedService so that enqueued services are aware of the
     * newly created bean.
     *
     * @param serviceDetails given service.
     */
    private void registerBeans(ServiceDetails serviceDetails) {
        for (Method beanMethod : serviceDetails.getBeans()) {
            ServiceBeanDetails beanDetails = new ServiceBeanDetails(beanMethod.getReturnType(), beanMethod, serviceDetails);
            this.instantiationService.createBeanInstance(beanDetails);

            ProxyUtils.createBeanProxyInstance(beanDetails);

            this.registerInstantiatedService(beanDetails);
        }
    }


    /**
     * @param cls given type.
     * @return true if allAvailableClasses contains a type
     * that is compatible with the given type.
     */
    private boolean isAssignableTypePresent(Class<?> cls) {
        for (Class<?> serviceType : this.allAvailableClasses) {
            if (cls.isAssignableFrom(serviceType)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Adds each service to the enqueuedServiceDetails.
     * Adds each service type to allAvailableClasses.
     * Adds all beans that can be loaded from each service
     * to allAvailableClasses.
     *
     * @param mappedServices set of mapped services and their information.
     */
    private void init(Set<ServiceDetails> mappedServices) {
        this.enqueuedServiceDetails.clear();
        this.allAvailableClasses.clear();
        this.instantiatedServices.clear();

        for (ServiceDetails serviceDetails : mappedServices) {
            this.enqueuedServiceDetails.add(new EnqueuedServiceDetails(serviceDetails));
            this.allAvailableClasses.add(serviceDetails.getServiceType());
            this.allAvailableClasses.addAll(
                    Arrays.stream(serviceDetails.getBeans())
                            .map(Method::getReturnType)
                            .collect(Collectors.toList())
            );
        }

        //If services are provided through config, add them to the list of available classes and instances.
        this.allAvailableClasses.addAll(this.configuration.getProvidedServices()
                .stream()
                .map(ServiceDetails::getServiceType)
                .collect(Collectors.toList())
        );

        for (ServiceDetails instantiatedService : this.configuration.getProvidedServices()) {
            this.registerInstantiatedService(instantiatedService);
        }

        this.setDependencyRequirements();
    }

    /**
     * Checks if the client has a service that will never be instantiated because
     * it has a dependency that is not present in the application context.
     * <p>
     * If {@link Nullable} annotation is present, the missing dependency is considered valid.
     */
    private void setDependencyRequirements() {
        for (EnqueuedServiceDetails enqueuedService : this.enqueuedServiceDetails) {
            for (Parameter parameter : enqueuedService.getServiceDetails().getTargetConstructor().getParameters()) {
                final Class<?> dependency = parameter.getType();

                if (this.isAssignableTypePresent(dependency)) {
                    continue;
                }

                if (AliasFinder.isAnnotationPresent(parameter.getDeclaredAnnotations(), Nullable.class)) {
                    enqueuedService.setDependencyNotNull(dependency, false);
                } else {
                    throw new ServiceInstantiationException(
                            String.format(COULD_NOT_FIND_CONSTRUCTOR_PARAM_MSG,
                                    enqueuedService.getServiceDetails().getServiceType().getName(),
                                    dependency.getName()
                            )
                    );
                }

                //Check for @Autowired annotated fields.
                for (Field autowireAnnotatedField : enqueuedService.getServiceDetails().getAutowireAnnotatedFields()) {
                    if (!this.isAssignableTypePresent(autowireAnnotatedField.getType())) {
                        throw new ServiceInstantiationException(
                                String.format(COULD_NOT_FIND_FIELD_PARAM_MSG,
                                        enqueuedService.getServiceDetails().getServiceType().getName(),
                                        autowireAnnotatedField.getType().getName()
                                )
                        );
                    }
                }
            }
        }
    }
}
