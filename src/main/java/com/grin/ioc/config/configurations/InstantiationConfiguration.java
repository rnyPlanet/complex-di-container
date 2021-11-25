package com.grin.ioc.config.configurations;

import com.grin.ioc.config.BaseConfiguration;
import com.grin.ioc.config.DIConfiguration;
import com.grin.ioc.constants.Constants;
import com.grin.ioc.models.ServiceDetails;

import java.util.ArrayList;
import java.util.Collection;

public class InstantiationConfiguration extends BaseConfiguration {

    private int maxNumberIteration;

    private Collection<ServiceDetails> providedServices;

    public InstantiationConfiguration(DIConfiguration parentConfig) {
        super(parentConfig);
        this.maxNumberIteration = Constants.MAX_NUMBER_OF_INSTANTIATION_ITERATIONS;

        this.providedServices = new ArrayList<>();
    }

    public int getMaxNumberIteration() {
        return this.maxNumberIteration;
    }

    public InstantiationConfiguration setMaxNumberIteration(int maxNumberIteration) {
        this.maxNumberIteration = maxNumberIteration;
        return this;
    }

    public InstantiationConfiguration addProvidedServices(Collection<ServiceDetails> serviceDetails) {
        this.providedServices.addAll(serviceDetails);
        return this;
    }

    public Collection<ServiceDetails> getProvidedServices() {
        return this.providedServices;
    }
}