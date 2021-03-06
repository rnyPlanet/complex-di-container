package com.grin.ioc.models;

import javassist.util.proxy.MethodHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MethodInvocationHandlerImpl implements MethodHandler {
    private ServiceDetails serviceDetails;

    public MethodInvocationHandlerImpl(ServiceDetails serviceDetails) {
        this.serviceDetails = serviceDetails;
    }

    @Override
    public Object invoke(Object o, Method method, Method method1, Object[] objects) throws Throwable {
        try {
            return method.invoke(this.serviceDetails.getActualInstance(), objects);
        } catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        }
    }
}
