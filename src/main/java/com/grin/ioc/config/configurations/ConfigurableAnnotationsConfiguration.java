package com.grin.ioc.config.configurations;

import com.grin.ioc.config.BaseConfiguration;
import com.grin.ioc.config.DIConfiguration;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ConfigurableAnnotationsConfiguration extends BaseConfiguration {

    private Set<Class<? extends Annotation>> serviceAnnotations;
    private Set<Class<? extends Annotation>> beanAnnotations;

    public ConfigurableAnnotationsConfiguration(DIConfiguration parentConfig) {
        super(parentConfig);
        this.serviceAnnotations = new HashSet<>();
        this.beanAnnotations = new HashSet<>();
    }

    public ConfigurableAnnotationsConfiguration addServiceAnnotation(Class<? extends Annotation> annotation) {
        this.serviceAnnotations.add(annotation);
        return this;
    }

    public ConfigurableAnnotationsConfiguration addServiceAnnotations(Class<? extends Annotation>... annotations) {
        this.serviceAnnotations.addAll(Arrays.asList(annotations));
        return this;
    }

    public ConfigurableAnnotationsConfiguration addBeanAnnotation(Class<? extends Annotation> annotation) {
        this.beanAnnotations.add(annotation);
        return this;
    }
    public ConfigurableAnnotationsConfiguration addBeanAnnotations(Class<? extends Annotation> annotations) {
        this.beanAnnotations.addAll(Arrays.asList(annotations));
        return this;
    }

    public Set<Class<? extends Annotation>> getServiceAnnotations() {
        return serviceAnnotations;
    }

    public Set<Class<? extends Annotation>> getBeanAnnotations() {
        return beanAnnotations;
    }
}
