package edu.nwpu.cpuis;

import io.github.swagger2markup.Swagger2MarkupConfig;
import io.github.swagger2markup.Swagger2MarkupConverter;
import io.github.swagger2markup.builder.Swagger2MarkupConfigBuilder;
import io.github.swagger2markup.markup.builder.MarkupLanguage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import java.net.URL;
import java.nio.file.Paths;

@SpringBootTest
public class SwaggerTest {
    @Test
    public void generateAsciiDocs(@Value ("${server.port}") String port) throws Exception {
        //    输出Ascii格式
        Swagger2MarkupConfig config = new Swagger2MarkupConfigBuilder ()
                .withMarkupLanguage (MarkupLanguage.ASCIIDOC)
                .build ();
        //此处填写swagger项目地址
        Swagger2MarkupConverter.from (new URL (String.format ("http://127.0.0.1:%s/v2/api-docs", port)))
                .withConfig (config)
                .build ()
                .toFile (Paths.get ("src/docs/asciidoc/generated/all"));
    }
}
