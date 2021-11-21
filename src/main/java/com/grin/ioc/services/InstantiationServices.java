package com.grin.ioc.services;

import com.grin.ioc.exceptions.ServiceInstantiationException;
import com.grin.ioc.models.ServiceDetails;

import java.util.List;
import java.util.Set;

public interface InstantiationServices {
    List<ServiceDetails> instantiateServicesAndBeans(Set<ServiceDetails> mappedServices) throws ServiceInstantiationException;
}