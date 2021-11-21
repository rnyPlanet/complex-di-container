package com.grin.ioc.config;

import com.grin.ioc.config.configurations.ConfigurableAnnotationsConfiguration;

public class DIConfiguration {
    private ConfigurableAnnotationsConfiguration annotations;

    public DIConfiguration() {
        this.annotations = new ConfigurableAnnotationsConfiguration(this);
    }

    public ConfigurableAnnotationsConfiguration annotations() {
        return this.annotations;
    }

    public DIConfiguration build() {
        return this;
    }
}
