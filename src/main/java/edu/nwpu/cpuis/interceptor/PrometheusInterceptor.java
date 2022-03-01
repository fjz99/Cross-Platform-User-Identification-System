package edu.nwpu.cpuis.interceptor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@Component
public class PrometheusInterceptor implements HandlerInterceptor {
    @Autowired
    private MeterRegistry registry;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String method = request.getMethod ();
        String requestURI = request.getRequestURI ();

        Counter counter = registry.counter ("http_requests_total", "URI", requestURI, "method", method,
                "code", String.valueOf (response.getStatus ()));
        counter.increment ();
    }

}
