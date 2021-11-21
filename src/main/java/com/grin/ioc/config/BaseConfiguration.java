package com.grin.ioc.config;

import com.grin.ioc.config.configurations.ConfigurableAnnotationsConfiguration;

public abstract class BaseConfiguration {
    private DIConfiguration parentConfig;

    protected BaseConfiguration(DIConfiguration parentConfig) {
        this.parentConfig = parentConfig;
    }

    public DIConfiguration and() {
        return this.parentConfig;
    }
}
