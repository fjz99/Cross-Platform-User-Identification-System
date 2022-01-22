package edu.nwpu.cpuis.interceptor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @date 2022/1/21 18:59
 */
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
        String e = ex == null ? "None" : ex.toString ();
        String methodName = "None";
        if (handler instanceof HandlerMethod) {
            methodName = ((HandlerMethod) handler).getBeanType ().getCanonicalName () + "#"
                    + ((HandlerMethod) handler).getMethod ().getName ();
        }
        Counter counter = registry.counter ("http_requests_total", "URI", requestURI, "method", method,
                "code", String.valueOf (response.getStatus ()), "exception", e, "handler", methodName);
        counter.increment ();
    }

}
