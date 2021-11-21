package com.grin.ioc.services;

import com.grin.ioc.models.ServiceDetails;

import java.util.Set;

public interface ServicesScanningService {

    Set<ServiceDetails<?>> mapServices(Set<Class<?>> locatedClasses);

}
