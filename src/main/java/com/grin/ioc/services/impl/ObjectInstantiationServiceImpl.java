package com.grin.ioc.services.impl;

import com.grin.ioc.exceptions.BeanInstantiationException;
import com.grin.ioc.exceptions.PostConstructException;
import com.grin.ioc.exceptions.PreDestroyException;
import com.grin.ioc.exceptions.ServiceInstantiationException;
import com.grin.ioc.models.ServiceBeanDetails;
import com.grin.ioc.models.ServiceDetails;
import com.grin.ioc.services.ObjectInstantiationService;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ObjectInstantiationServiceImpl implements ObjectInstantiationService {
    private static final String INVALID_PARAMETERS_COUNT_MSG = "Invalid parameters count for '%s'.";

    @Override
    public void createInstance(ServiceDetails serviceDetails, Object... constructorParams) throws ServiceInstantiationException {
        Constructor<?> targetConstructor = serviceDetails.getTargetConstructor();

        if (constructorParams.length != targetConstructor.getParameterCount()) {
            throw new ServiceInstantiationException(String.format(INVALID_PARAMETERS_COUNT_MSG, serviceDetails.getServiceType().getName()));
        }

        try {
            Object instance = targetConstructor.newInstance(constructorParams);
            serviceDetails.setInstance(instance);
            this.invokePostConstruck(serviceDetails);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new ServiceInstantiationException(e.getMessage(), e);
        }

    }

    private void invokePostConstruck(ServiceDetails serviceDetails) throws PostConstructException {
        if (serviceDetails.getPostConstructMethod() == null) {
            return;
        }

        try {
            serviceDetails.getPostConstructMethod().invoke(serviceDetails.getInstance());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new PostConstructException(e.getMessage(), e);
        }
    }

    @Override
    public void createBeanInstance(ServiceBeanDetails serviceBeanDetails) throws BeanInstantiationException {
        Method originMethod = serviceBeanDetails.getOriginMethod();
        Object rootIstance = serviceBeanDetails.getRootService().getInstance();

        try {
            Object instance = originMethod.invoke(rootIstance);
            serviceBeanDetails.setInstance(instance);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new BeanInstantiationException(e.getMessage(), e);
        }
    }

    @Override
    public void destroyInstance(ServiceDetails serviceDetails) throws PreDestroyException {
        if (serviceDetails.getPreDestroyMethod() != null) {
            try {
                serviceDetails.getPreDestroyMethod().invoke(serviceDetails.getInstance());
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new PreDestroyException(e.getMessage(), e);
            }
        }

        serviceDetails.setInstance(null);
    }
}