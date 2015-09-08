package org.restler.spring;

import com.google.common.collect.ImmutableMultimap;
import org.restler.client.ParameterResolver;
import org.restler.client.RestlerException;
import org.restler.client.ServiceMethod;
import org.restler.client.ServiceMethodInvocation;
import org.restler.http.HttpMethod;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.util.UriComponentsBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps a properly annotated Java method invocation to invocation of a service method.
 */
public class ControllerMethodInvocationMapper implements BiFunction<Method, Object[], ServiceMethodInvocation<?>> {

    private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    private static final Pattern pathVariablesPattern = Pattern.compile("\\{([-a-zA-Z0-9@:%_\\+.~#?&/=]*)\\}");

    private final URI baseUrl;
    private final ParameterResolver paramResolver;

    public ControllerMethodInvocationMapper(URI baseUrl, ParameterResolver paramResolver) {
        this.baseUrl = baseUrl;
        this.paramResolver = paramResolver;
    }

    @Override
    public ServiceMethodInvocation<?> apply(Method method, Object[] args) {
        Object requestBody = null;
        Map<String, Object> pathVariables = new HashMap<>();
        ImmutableMultimap.Builder<String, String> requestParams = new ImmutableMultimap.Builder<>();

        Annotation[][] parametersAnnotations = method.getParameterAnnotations();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);

        InvocationParamResolver resolver = new InvocationParamResolver(method, args, parametersAnnotations, parameterNames, paramResolver);
        for (int pi = 0; pi < parametersAnnotations.length; pi++) {
            for (int ai = 0; ai < parametersAnnotations[pi].length; ai++) {
                Annotation annotation = parametersAnnotations[pi][ai];
                if (annotation instanceof PathVariable) {

                    String pathVariableName = ((PathVariable) annotation).value();
                    if (StringUtils.isEmpty(pathVariableName) && parameterNames != null)
                        pathVariableName = parameterNames[pi];
                    if (StringUtils.isEmpty(pathVariableName))
                        throw new RuntimeException("Name of a path variable can't be resolved during the method " + method + " call");

                    pathVariables.put(pathVariableName, resolver.resolve(pi).orElseGet(() -> null));

                } else if (annotation instanceof RequestParam) {

                    String parameterVariableName;
                    if (!StringUtils.isEmpty(((RequestParam) annotation).value())) {
                        parameterVariableName = ((RequestParam) annotation).value();
                    } else if (parameterNames != null && parameterNames[pi] != null) {
                        parameterVariableName = parameterNames[pi];
                    } else {
                        throw new RuntimeException("Name of a request parameter can't be resolved during the method " + method + " call");
                    }

                    resolver.resolve(pi).
                            ifPresent(param -> requestParams.put(parameterVariableName, param));

                } else if (annotation instanceof RequestBody) {
                    requestBody = args[pi];
                }
            }
        }

        ServiceMethod<?> description = getDescription(method);
        List<String> unboundPathVariables = unusedPathVariables(pathVariables, description.getUriTemplate());
        if (unboundPathVariables.size() > 0) {
            throw new RestlerException("You should introduce method parameter with @PathVariable annotation for each url template variable. Unbound variables: " + unboundPathVariables);
        }
        return new ServiceMethodInvocation<>(baseUrl, description, requestBody, pathVariables, requestParams.build());
    }

    private ServiceMethod<?> getDescription(Method method) {

        RequestMapping controllerMapping = method.getDeclaringClass().getDeclaredAnnotation(RequestMapping.class);
        RequestMapping methodMapping = method.getDeclaredAnnotation(RequestMapping.class);
        if (methodMapping == null) {
            throw new RuntimeException("The method " + method + " is not mapped");
        }

        ResponseBody methodResponseBodyAnnotation = AnnotationUtils.findAnnotation(method, ResponseBody.class);
        ResponseBody classResponseBodyAnnotation = AnnotationUtils.findAnnotation(method.getDeclaringClass(), ResponseBody.class);
        if (methodResponseBodyAnnotation == null && classResponseBodyAnnotation == null) {
            throw new RuntimeException("The method " + method + " does not return response body");
        }

        RequestMethod declaredMethod;
        if (methodMapping.method() == null || methodMapping.method().length == 0) {
            declaredMethod = RequestMethod.GET;
        } else {
            declaredMethod = methodMapping.method()[0];
        }
        HttpMethod httpMethod = HttpMethod.valueOf(declaredMethod.toString());

        HttpStatus expectedStatus = HttpStatus.OK;
        ResponseStatus statusAnnotation = method.getDeclaredAnnotation(ResponseStatus.class);
        if (statusAnnotation != null) {
            expectedStatus = statusAnnotation.value();
        }

        String uriTemplate = UriComponentsBuilder.fromUriString("/").pathSegment(getMappedUriString(controllerMapping), getMappedUriString(methodMapping)).build().toUriString();

        Class<?> resultType = method.getReturnType();
        Type returnType = method.getGenericReturnType();

        if (resultType == DeferredResult.class || resultType == Callable.class) {
            ParameterizedType parameterizedType = (ParameterizedType) returnType;
            returnType = parameterizedType.getActualTypeArguments()[0];
        }

        return new ServiceMethod<>(uriTemplate, returnType, httpMethod, expectedStatus.value());
    }

    private List<String> unusedPathVariables(Map<String, Object> pathVariables, String uriTemplate) {
        List<String> res = new ArrayList<>();
        Matcher matcher = pathVariablesPattern.matcher(uriTemplate);
        while (matcher.find()) {
            if (!pathVariables.containsKey(matcher.group(1))) {
                res.add(matcher.group(1));
            }
        }
        return res;
    }

    private String getMappedUriString(RequestMapping mapping) {
        if (mapping == null) {
            return "";
        } else {
            return getFirstOrEmpty(mapping.value());
        }
    }

    private String getFirstOrEmpty(String[] strings) {
        if (strings == null || strings.length == 0) {
            return "";
        } else {
            return strings[0];
        }
    }

    private class InvocationParamResolver {

        private final Method method;
        private final Object[] args;
        private final Annotation[][] annotations;
        private final String[] paramNames;

        private final ParameterResolver paramResolver;

        public InvocationParamResolver(Method method, Object[] args, Annotation[][] annotations, String[] paramNames, ParameterResolver paramResolver) {
            this.paramResolver = paramResolver;
            this.paramNames = paramNames;
            this.annotations = annotations;
            this.args = args;
            this.method = method;
        }

        public Optional<String> resolve(int paramIdx) {
            return paramResolver.resolve(method, args, annotations, paramNames, paramIdx);
        }
    }
}
