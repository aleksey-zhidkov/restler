package org.restler.client;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * A CGLib implementation of {@link ClientFactory} that uses {@link ServiceMethodInvocationExecutor} for execution client methods.
 */
@SuppressWarnings("unchecked")
public class CGLibClientFactory implements ClientFactory {

    private final ServiceMethodInvocationExecutor sMvcExecutor;
    private final ServiceMethodInvocationExecutor sdrExecutor;
    private final InvocationMapper controllerInvocationMapper;
    private final InvocationMapper repositoryInvocationMapper;

    private final Executor threadExecutor;

    private final HashMap<Class<?>, Function<ServiceMethodInvocation<?>, ?>> invocationExecutors;
    private final Function<ServiceMethodInvocation<?>, ?> defaultInvocationExecutor;

    public CGLibClientFactory(ServiceMethodInvocationExecutor sMvcExecutor, ServiceMethodInvocationExecutor sdrExecutor, InvocationMapper controllerInvocationMapper,
                              InvocationMapper repositoryInvocationMapper, Executor threadExecutor) {
        this.sMvcExecutor = sMvcExecutor;
        this.sdrExecutor = sdrExecutor;

        this.repositoryInvocationMapper = repositoryInvocationMapper;
        this.controllerInvocationMapper = controllerInvocationMapper;

        this.threadExecutor = threadExecutor;

        invocationExecutors = new HashMap<>();
        invocationExecutors.put(DeferredResult.class, new DeferredResultInvocationExecutor());
        invocationExecutors.put(Callable.class, new CallableResultInvocationExecutor());

        defaultInvocationExecutor = sMvcExecutor::execute;
    }

    @Override
    public <C> C produceClient(Class<C> controllerOrRepositoryClass) {

        Controller controllerAnnotation = controllerOrRepositoryClass.getDeclaredAnnotation(Controller.class);
        RestController restControllerAnnotation = controllerOrRepositoryClass.getDeclaredAnnotation(RestController.class);

        boolean isRepository = isRepository(controllerOrRepositoryClass);
        boolean isController = controllerAnnotation != null || restControllerAnnotation != null;

        if (!isController && !isRepository) {
            throw new IllegalArgumentException("Not a controller or a repository");
        }

        InvocationHandler handler;
        if (isRepository) {
            handler = new RepositoryMethodInvocationHandler();
        } else {
            handler = new ControllerMethodInvocationHandler();
        }

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(controllerOrRepositoryClass);
        enhancer.setCallback(handler);

        return (C) enhancer.create();
    }

    private Function<ServiceMethodInvocation<?>, ?> getInvocationExecutor(Method method) {
        Function<ServiceMethodInvocation<?>, ?> invocationExecutor = invocationExecutors.get(method.getReturnType());
        if (invocationExecutor == null) {
            invocationExecutor = defaultInvocationExecutor;
        }
        return invocationExecutor;
    }

    private boolean isRepository(Class<?> someClass) {
        if (someClass.isInterface()) {

            if (someClass == Repository.class) {
                return true;
            }

            Class<?>[] interfaces = someClass.getInterfaces();

            for (Class<?> interf : interfaces) {
                if (isRepository(interf)) {
                    return true;
                }
            }
        }
        return false;
    }

    private class ControllerMethodInvocationHandler implements InvocationHandler {

        @Override
        public Object invoke(Object o, Method method, Object[] args) throws Throwable {
            ServiceMethodInvocation<?> invocation = controllerInvocationMapper.apply(o, method, args);

            return getInvocationExecutor(method).apply(invocation);
        }
    }

    private class RepositoryMethodInvocationHandler implements InvocationHandler {

        @Override
        public Object invoke(Object o, Method method, Object[] args) throws Throwable {
            ServiceMethodInvocation<?> invocation = repositoryInvocationMapper.apply(o, method, args);
            return sdrExecutor.execute(invocation);
        }
    }

    private class DeferredResultInvocationExecutor implements Function<ServiceMethodInvocation<?>, DeferredResult<?>> {

        @Override
        public DeferredResult apply(ServiceMethodInvocation<?> serviceMethodInvocation) {
            DeferredResult deferredResult = new DeferredResult();
            threadExecutor.execute(() -> deferredResult.setResult(sMvcExecutor.execute(serviceMethodInvocation)));
            return deferredResult;
        }
    }

    private class CallableResultInvocationExecutor implements Function<ServiceMethodInvocation<?>, Callable<?>> {

        @Override
        public Callable<?> apply(ServiceMethodInvocation<?> serviceMethodInvocation) {
            return () -> sMvcExecutor.execute(serviceMethodInvocation);
        }
    }
}
