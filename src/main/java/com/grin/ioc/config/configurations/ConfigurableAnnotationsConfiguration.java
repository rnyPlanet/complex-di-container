package com.grin.ioc.config.configurations;

import com.grin.ioc.config.BaseConfiguration;
import com.grin.ioc.config.DIConfiguration;

import java.lang.annotation.Annotation;
import java.util.*;

public class ConfigurableAnnotationsConfiguration extends BaseConfiguration {

    private Set<Class<? extends Annotation>> serviceAnnotations;
    private Set<Class<? extends Annotation>> beanAnnotations;
    private Map<Class<?>, Class<? extends Annotation>> additionalClasses;

    public ConfigurableAnnotationsConfiguration(DIConfiguration parentConfig) {
        super(parentConfig);
        this.serviceAnnotations = new HashSet<>();
        this.beanAnnotations = new HashSet<>();
        this.additionalClasses = new HashMap<>();
    }

    public ConfigurableAnnotationsConfiguration addServiceAnnotation(Class<? extends Annotation> annotation) {
        this.serviceAnnotations.add(annotation);
        return this;
    }

    public ConfigurableAnnotationsConfiguration addServiceAnnotations(Collection<Class<? extends Annotation>> annotations) {
        this.serviceAnnotations.addAll(Set.copyOf(annotations));
        return this;
    }

    public ConfigurableAnnotationsConfiguration addBeanAnnotation(Class<? extends Annotation> annotation) {
        this.beanAnnotations.add(annotation);
        return this;
    }
    public ConfigurableAnnotationsConfiguration addBeanAnnotations(Collection<Class<? extends Annotation>> annotations) {
        this.beanAnnotations.addAll(Set.copyOf(annotations));
        return this;
    }

    public ConfigurableAnnotationsConfiguration addAdditionalClassesForScanning(Map<Class<?>, Class<? extends Annotation>> additionalClasses) {
        this.additionalClasses.putAll(additionalClasses);
        return this;
    }

    public Set<Class<? extends Annotation>> getServiceAnnotations() {
        return serviceAnnotations;
    }

    public Set<Class<? extends Annotation>> getBeanAnnotations() {
        return beanAnnotations;
    }

    public Map<Class<?>, Class<? extends Annotation>> getAdditionalClasses() {
        return this.additionalClasses;
    }
}
