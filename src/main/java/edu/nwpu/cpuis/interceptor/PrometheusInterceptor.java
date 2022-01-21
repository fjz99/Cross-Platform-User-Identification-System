package edu.nwpu.cpuis.interceptor;

import io.prometheus.client.Counter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

/**
 * @date 2022/1/21 18:59
 */
@Component
public class PrometheusInterceptor implements HandlerInterceptor {
    @Autowired
    private RequestMappingHandlerMapping mapping;

    private static final Counter requestSuccess = Counter.build ()
            .name ("SuccessQueries")
            .labelNames ("handler")
            .help ("请求次数")
            .register ();
    private static final Counter requestTotal = Counter.build ()
            .name ("TotalQueries")
            .labelNames ("handler")
            .help ("请求次数")
            .register ();
    private static final Counter requestFailed = Counter.build ()
            .name ("FailedQueries")
            .labelNames ("handler")
            .help ("请求次数")
            .register ();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        assert handler instanceof Method;
        requestTotal.labels ("total").inc ();
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (ex == null) {
            requestTotal.labels ("success").inc ();
        } else {
            requestTotal.labels ("failure").inc ();
        }
    }

}
