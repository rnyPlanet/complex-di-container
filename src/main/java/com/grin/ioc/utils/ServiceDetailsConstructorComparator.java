package com.grin.ioc.utils;

import com.grin.ioc.models.ServiceDetails;

import java.util.Comparator;

public class ServiceDetailsConstructorComparator implements Comparator<ServiceDetails> {
    @Override
    public int compare(ServiceDetails serviceDetails1, ServiceDetails serviceDetails2) {
        return Integer.compare(
                serviceDetails1.getTargetConstructor().getParameterCount(),
                serviceDetails2.getTargetConstructor().getParameterCount()
        );
    }
}
