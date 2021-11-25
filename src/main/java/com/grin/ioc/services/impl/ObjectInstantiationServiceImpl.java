package com.grin.ioc.services.impl;

import com.grin.ioc.exceptions.BeanInstantiationException;
import com.grin.ioc.exceptions.PostConstructException;
import com.grin.ioc.exceptions.PreDestroyException;
import com.grin.ioc.exceptions.ServiceInstantiationException;
import com.grin.ioc.models.ServiceBeanDetails;
import com.grin.ioc.models.ServiceDetails;
import com.grin.ioc.services.ObjectInstantiationService;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * {@link ObjectInstantiationService} implementation.
 */
public class ObjectInstantiationServiceImpl implements ObjectInstantiationService {
    private static final String INVALID_PARAMETERS_COUNT_MSG = "Invalid parameters count for '%s'.";

    /**
     * Creates an instance for a service.
     * Invokes the PostConstruct method.
     *
     * @param serviceDetails    the given service details.
     * @param constructorParams instantiated dependencies.
     */
    @Override
    public void createInstance(ServiceDetails serviceDetails, Object[] constructorParams, Object[] autowiredFieldInstances) throws ServiceInstantiationException {
        Constructor<?> targetConstructor = serviceDetails.getTargetConstructor();

        if (constructorParams.length != targetConstructor.getParameterCount()) {
            throw new ServiceInstantiationException(String.format(INVALID_PARAMETERS_COUNT_MSG, serviceDetails.getServiceType().getName()));
        }

        try {
            Object instance = targetConstructor.newInstance(constructorParams);
            serviceDetails.setInstance(instance);
            this.setAutowiredFieldInstances(serviceDetails, autowiredFieldInstances);
            this.invokePostConstruct(serviceDetails);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new ServiceInstantiationException(e.getMessage(), e);
        }

    }

    /**
     * Iterates all {@link com.grin.ioc.annotations.Autowired} annotated fields and sets them a given instance.
     *
     * @param serviceDetails          - given service details.
     * @param autowiredFieldInstances - field instances.
     */
    private void setAutowiredFieldInstances(ServiceDetails serviceDetails, Object[] autowiredFieldInstances) throws IllegalAccessException {
        Field[] autowireAnnotatedFields = serviceDetails.getAutowireAnnotatedFields();

        for (int i = 0; i < autowireAnnotatedFields.length; i++) {
            autowireAnnotatedFields[i].set(serviceDetails.getActualInstance(), autowiredFieldInstances[i]);
        }
    }

    /**
     * Invokes post construct method if one is present for a given service.
     *
     * @param serviceDetails - the given service.
     */
    private void invokePostConstruct(ServiceDetails serviceDetails) throws PostConstructException {
        if (serviceDetails.getPostConstructMethod() == null) {
            return;
        }

        try {
            serviceDetails.getPostConstructMethod().invoke(serviceDetails.getActualInstance());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new PostConstructException(e.getMessage(), e);
        }
    }

    /**
     * Creates an instance for a bean by invoking its origin method
     * and passing the instance of the service in which the bean has been declared.
     *
     * @param serviceBeanDetails the given bean details.
     */
    @Override
    public void createBeanInstance(ServiceBeanDetails serviceBeanDetails) throws BeanInstantiationException {
        Method originMethod = serviceBeanDetails.getOriginMethod();
        Object rootIstance = serviceBeanDetails.getRootService().getActualInstance();

        try {
            Object instance = originMethod.invoke(rootIstance);
            serviceBeanDetails.setInstance(instance);
            serviceBeanDetails.setProxyInstance(instance);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new BeanInstantiationException(e.getMessage(), e);
        }
    }

    /**
     * Sets the instance to null.
     * Invokes pre destroy method for the given service details if one is present.
     *
     * @param serviceDetails given service details.
     */
    @Override
    public void destroyInstance(ServiceDetails serviceDetails) throws PreDestroyException {
        if (serviceDetails.getPreDestroyMethod() != null) {
            try {
                serviceDetails.getPreDestroyMethod().invoke(serviceDetails.getActualInstance());
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new PreDestroyException(e.getMessage(), e);
            }
        }

        serviceDetails.setInstance(null);
    }
}