package site.cpuis;

import io.github.swagger2markup.GroupBy;
import io.github.swagger2markup.Language;
import io.github.swagger2markup.Swagger2MarkupConfig;
import io.github.swagger2markup.Swagger2MarkupConverter;
import io.github.swagger2markup.builder.Swagger2MarkupConfigBuilder;
import io.github.swagger2markup.markup.builder.MarkupLanguage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URL;
import java.nio.file.Paths;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class SwaggerTest {

    @Test
    public void generateAsciiDocs(@Value("${server.port}") String port) throws Exception {
        // 输出Ascii格式
        Swagger2MarkupConfig config = new Swagger2MarkupConfigBuilder ().withMarkupLanguage (MarkupLanguage.ASCIIDOC)
                .withOutputLanguage (Language.ZH).withPathsGroupedBy (GroupBy.TAGS).withGeneratedExamples ()
                .withoutInlineSchema ().build ();
//        String officialUrl = "https://petstore.swagger.io/v2/swagger.json";
//        Swagger2MarkupConverter.from (new URL (officialUrl)).withConfig (config)
//                .build ().toFolder (Paths.get ("src/docs/asciidoc/generated"));
        Swagger2MarkupConverter.from (new URL (String.format ("http://127.0.0.1:%s/v2/api-docs", port))).withConfig (config)
                .build ().toFolder (Paths.get ("src/docs/asciidoc/generated"));
    }
}
