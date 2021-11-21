package com.grin.ioc.config.configurations;

import com.grin.ioc.config.BaseConfiguration;
import com.grin.ioc.config.DIConfiguration;
import com.grin.ioc.constants.Constants;

public class InstantiationConfiguration extends BaseConfiguration {

    private int maxNumberIteration;

    public InstantiationConfiguration(DIConfiguration parentConfig) {
        super(parentConfig);
        this.maxNumberIteration = Constants.MAX_NUMBER_OF_INSTANTIATION_ITERATIONS;
    }

    public int getMaxNumberIteration() {
        return this.maxNumberIteration;
    }

    public InstantiationConfiguration setMaxNumberIteration(int maxNumberIteration) {
        this.maxNumberIteration = maxNumberIteration;
        return this;
    }
}