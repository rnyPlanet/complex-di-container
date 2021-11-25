package com.grin.ioc.models;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * Simple POJO class that keeps information about a service, its
 * required dependencies and the ones that are already resolved.
 */
public class EnqueuedServiceDetails {

    /**
     * Reference to the target service.
     */
    private ServiceDetails serviceDetails;

    /**
     * Array of dependencies that the target constructor of the service requires.
     */
    private Class<?>[] dependencies;

    /**
     * Keeps track for each dependency whether it is required
     */
    private boolean[] dependenciesRequirement;

    /**
     * Array of instances matching the types in @dependencies.
     */
    private Object[] dependencyInstances;

    /**
     * Array of dependencies that are required from {@link com.grin.ioc.annotations.Autowired} annotated fields.
     */
    private Class<?>[] fieldDependencies;

    /**
     * Array of instances matching the types in @fieldDependencies
     */
    private Object[] fieldDependencyInstances;

    public EnqueuedServiceDetails(ServiceDetails serviceDetails) {
        this.serviceDetails = serviceDetails;
        this.dependencies = serviceDetails.getTargetConstructor().getParameterTypes();
        this.dependencyInstances = new Object[this.dependencies.length];
        this.dependenciesRequirement = new boolean[this.dependencies.length];

        Arrays.fill(this.dependenciesRequirement, true);

        this.fieldDependencies = new Class[this.serviceDetails.getAutowireAnnotatedFields().length];
        this.fieldDependencyInstances = new Object[this.serviceDetails.getAutowireAnnotatedFields().length];

        this.fillFieldDependencyTypes();
    }

    private void fillFieldDependencyTypes() {
        Field[] autowireAnnotatedFields = this.serviceDetails.getAutowireAnnotatedFields();

        for (int i = 0; i < autowireAnnotatedFields.length; i++) {
            this.fieldDependencies[i] = autowireAnnotatedFields[i].getType();
        }
    }

    public ServiceDetails getServiceDetails() {
        return this.serviceDetails;
    }

    public Class<?>[] getFieldDependencies() {
        return this.fieldDependencies;
    }

    public Object[] getFieldDependencyInstances() {
        return this.fieldDependencyInstances;
    }

    public Class<?>[] getDependencies() {
        return this.dependencies;
    }

    public Object[] getDependencyInstances() {
        return this.dependencyInstances;
    }

    /**
     * Adds the object instance in the array of instantiated dependencies
     * by keeping the exact same position as the target constructor of the service has it.
     *
     * @param instance the given dependency instance.
     */
    public void addDependencyInstance(Object instance) {
        Class<?> instanceType = instance.getClass();

        for (int i = 0; i < this.dependencies.length; i++) {
            if (this.dependencies[i].isAssignableFrom(instanceType)) {
                this.dependencyInstances[i] = instance;
                return;
            }
        }

        for (int i = 0; i < this.fieldDependencies.length; i++) {
            if (this.fieldDependencies[i].isAssignableFrom(instanceType)) {
                this.fieldDependencyInstances[i] = instance;
            }
        }
    }

    /**
     * Checks if all dependencies have corresponding instances.
     *
     * @return true of an dependency instances are available.
     */
    public boolean isResolved() {
        for (int i = 0; i < this.dependencyInstances.length; i++) {
            if (this.dependencyInstances[i] == null && this.dependenciesRequirement[i]) {
                return false;
            }
        }

        for (Object fieldDependencyInstance : this.fieldDependencyInstances) {
            if (fieldDependencyInstance == null) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if a given class type is present in the array of required dependencies.
     *
     * @param dependencyType - the given class type.
     * @return true if the given type is present in the array of required dependencies.
     */
    public boolean isDependencyRequired(Class<?> dependencyType) {
        for (Class<?> dependency : this.dependencies) {
            if (dependency.isAssignableFrom(dependencyType)) {
                return true;
            }
        }

        for (Class<?> fieldDependency : this.fieldDependencies) {
            if (fieldDependency.isAssignableFrom(dependencyType)) {
                return true;
            }
        }

        return false;
    }

    public void setDependencyNotNull(Class<?> dependencyType, boolean isRequired) {
        for (int i = 0; i < this.dependenciesRequirement.length; i++) {
            if (this.dependencies[i].isAssignableFrom(dependencyType)) {
                this.dependenciesRequirement[i] = isRequired;
                return;
            }
        }
    }

    @Override
    public String toString() {
        return this.serviceDetails.getServiceType().getName();
    }
}