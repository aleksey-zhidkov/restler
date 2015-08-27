package org.restler.http;


import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Type;

public class SpringDataRestOperationsExecutor implements RequestExecutor {

    private RestTemplate restTemplate;

    public SpringDataRestOperationsExecutor(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        restTemplate.getMessageConverters().add(0, new SpringDataRestMessageConverter());
    }

    public <T> ResponseEntity<T> execute(Request<T> request) {
        RequestEntity<?> requestEntity = request.toRequestEntity();
        return restTemplate.exchange(requestEntity, new ParameterizedTypeReference<T>() {
            @Override
            public Type getType() {
                return request.getGenericReturnType();
            }
        });
    }

}
