package com.grin.ioc.services;

import com.grin.ioc.exceptions.BeanInstantiationException;
import com.grin.ioc.exceptions.PreDestroyException;
import com.grin.ioc.exceptions.ServiceInstantiationException;
import com.grin.ioc.models.ServiceBeanDetails;
import com.grin.ioc.models.ServiceDetails;

public interface ObjectInstantiationService {

    void createInstance(ServiceDetails serviceDetails, Object[] constructorParams, Object[] autowiredFieldInstances) throws ServiceInstantiationException;

    void createBeanInstance(ServiceBeanDetails serviceBeanDetails) throws BeanInstantiationException;

    void destroyInstance(ServiceDetails serviceDetails) throws PreDestroyException;

}