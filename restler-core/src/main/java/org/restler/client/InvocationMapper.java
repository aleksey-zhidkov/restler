package org.restler.client;

import java.lang.reflect.Method;

public interface InvocationMapper {
    ServiceMethodInvocation<?> apply(Object o, Method method, Object[] args);
}
