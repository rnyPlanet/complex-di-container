package com.grin.ioc.config;

import com.grin.ioc.config.configurations.ConfigurableAnnotationsConfiguration;
import com.grin.ioc.config.configurations.InstantiationConfiguration;

public class DIConfiguration {
    private ConfigurableAnnotationsConfiguration annotations;
    private InstantiationConfiguration instantitations;

    public DIConfiguration() {
        this.annotations = new ConfigurableAnnotationsConfiguration(this);
        this.instantitations = new InstantiationConfiguration(this);
    }

    public ConfigurableAnnotationsConfiguration annotations() {
        return this.annotations;
    }

    public InstantiationConfiguration instantiations() {
        return this.instantitations;
    }

    public DIConfiguration build() {
        return this;
    }
}
