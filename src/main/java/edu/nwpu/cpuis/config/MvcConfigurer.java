package edu.nwpu.cpuis.config;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;


@Configuration
public class MvcConfigurer implements WebMvcConfigurer {
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        WebMvcConfigurer.super.configureMessageConverters (converters);
        FastJsonHttpMessageConverter fastJsonConverter = new FastJsonHttpMessageConverter ();
        FastJsonConfig fastJsonConfig = new FastJsonConfig ();
        //过滤并修改配置返回内容
        fastJsonConfig.setSerializerFeatures (
                //List字段如果为null,输出为[],而非null
                SerializerFeature.WriteNullListAsEmpty,
                //字符类型字段如果为null,输出为"",而非null
                SerializerFeature.WriteNullStringAsEmpty,
                //Boolean字段如果为null,输出为falseJ,而非null
                //SerializerFeature.WriteNullBooleanAsFalse,
                //消除对同一对象循环引用的问题，默认为false（如果不配置有可能会进入死循环）
                SerializerFeature.DisableCircularReferenceDetect,
                //是否输出值为null的字段,默认为false。
                SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullBooleanAsFalse,
                SerializerFeature.WriteNonStringKeyAsString,
                SerializerFeature.WriteNonStringValueAsString
        );
        //处理中文乱码问题
        List<MediaType> fastMediaTypes = new ArrayList<> ();
        fastMediaTypes.add (MediaType.APPLICATION_JSON);
        fastJsonConverter.setSupportedMediaTypes (fastMediaTypes);
        fastJsonConverter.setFastJsonConfig (fastJsonConfig);
        //将fastjson添加到视图消息转换器列表内
        converters.add (fastJsonConverter);
    }

    private CorsConfiguration corsConfig() {
        CorsConfiguration corsConfiguration = new CorsConfiguration ();
        corsConfiguration.addAllowedOrigin ("*");
//        corsConfiguration.addAllowedOriginPattern ();
        corsConfiguration.addAllowedHeader ("*");
        corsConfiguration.addAllowedMethod ("*");
//        corsConfiguration.setAllowCredentials (false); //用于cookie，不允许cookie
        corsConfiguration.setMaxAge (3600L);
        return corsConfiguration;
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource ();
        source.registerCorsConfiguration ("/**", corsConfig ());
        return new CorsFilter (source);
    }
}
