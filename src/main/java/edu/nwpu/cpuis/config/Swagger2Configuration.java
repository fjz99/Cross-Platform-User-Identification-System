package edu.nwpu.cpuis.config;

import com.google.common.base.Predicates;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @author fujiazheng
 */
@Configuration
@EnableSwagger2
//@SuppressWarnings ("unchecked")
public class Swagger2Configuration {

    private static final Contact CONTACT = new Contact ("fujiazheng", null, "1358925318@qq.com");
    private static final String VERSION = "1.0";

    @Bean
    public Docket buildDocket() {
        return new Docket (DocumentationType.SWAGGER_2)
                .pathMapping ("/")
                .apiInfo (buildApiInf ())
                .select ()
//                .apis (RequestHandlerSelectors.basePackage ("edu.nwpu.cpuis.controller"))
                .paths (PathSelectors.regex("/model/.*"))
//                .paths (PathSelectors.any ())
                .build ();
    }

    private ApiInfo buildApiInf() {
        return new ApiInfoBuilder ()
                .title ("api doc of cpuis")
                .description ("api doc")
                .contact (CONTACT)
                .version (VERSION)
                .build ();
    }
}
