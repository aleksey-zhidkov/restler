package org.restler.client;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class RepositoryMethodInvocationMapper implements InvocationMapper {

    private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private final URI baseUrl;

    public RepositoryMethodInvocationMapper(URI baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public ServiceMethodInvocation<?> apply(Object o, Method method, Object[] args) {
        ServiceMethod<?> description = getDescription(o, method);
        Object requestBody = null;
        Map<String, Object> pathVariables = new HashMap();
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap();


        Annotation[][] parametersAnnotations = method.getParameterAnnotations();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);

        for (int pi = 0; pi < parametersAnnotations.length; pi++) {
            for (int ai = 0; ai < parametersAnnotations[pi].length; ai++) {
                Annotation annotation = parametersAnnotations[pi][ai];
                if (annotation instanceof Param) {
                    String pathVariableName = ((Param) annotation).value();
                    if (StringUtils.isEmpty(pathVariableName) && parameterNames != null)
                        pathVariableName = parameterNames[pi];
                    if (StringUtils.isEmpty(pathVariableName))
                        throw new RuntimeException("Name of a path variable can't be resolved during the method " + method + " call");

                    requestParams.add(pathVariableName, args[pi].toString());
                }
            }
        }

        return new ServiceMethodInvocation<>(baseUrl, description, requestBody, pathVariables, requestParams);
    }

    private ServiceMethod<?> getDescription(Object o, Method method) {

        RepositoryRestResource repositoryAnnotation = o.getClass().getInterfaces()[0].getDeclaredAnnotation(RepositoryRestResource.class);
        RestResource methodAnnotation = method.getDeclaredAnnotation(RestResource.class);

        String methodMappedUriString;
        HttpMethod httpMethod;

        if (isCrudMethod(method)) {
            /*methodMappedUriString = "";
            httpMethod = HttpMethod.GET;*/
            throw new UnsupportedOperationException("Crud methods not supported");
        } else {
            methodMappedUriString = getQueryMethodUri(method, methodAnnotation);
            httpMethod = HttpMethod.GET;
        }

        String uriTemplate = UriComponentsBuilder.fromUriString("/").pathSegment(getRepositoryUri(o, repositoryAnnotation), methodMappedUriString).build().toUriString();

        Class<?> returnType = method.getReturnType();

        HttpStatus expectedStatus = HttpStatus.OK;

        return new ServiceMethod<>(uriTemplate, returnType, method.getGenericReturnType(), httpMethod, expectedStatus);
    }

    private String getRepositoryUri(Object o, RepositoryRestResource repositoryAnnotation) {

        String repositoryUriString;
        if (repositoryAnnotation == null || repositoryAnnotation.path().isEmpty()) {
            Type entityType = ((ParameterizedType) o.getClass().getInterfaces()[0].getGenericInterfaces()[0]).getActualTypeArguments()[0];
            try {
                repositoryUriString = Class.forName(entityType.getTypeName()).getSimpleName().toLowerCase() + "s";
            } catch (ClassNotFoundException e) {
                throw new RestlerException("Could not find class for repository's entity type", e);
            }
        } else {
            repositoryUriString = repositoryAnnotation.path();
        }

        return repositoryUriString;
    }

    private String getQueryMethodUri(Method method, RestResource methodAnnotation) {
        String methodName = method.getName();

        if (methodAnnotation != null && !methodAnnotation.path().isEmpty()) {
            methodName = methodAnnotation.path();
        }

        return "search/" + methodName;
    }

    private boolean isCrudMethod(Method method) {
        Method[] crudMethods = CrudRepository.class.getMethods();

        for (Method crudMethod : crudMethods) {
            if (crudMethod.equals(method)) {
                return true;
            }
        }

        return false;
    }
}
