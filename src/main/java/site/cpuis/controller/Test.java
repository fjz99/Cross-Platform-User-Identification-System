package site.cpuis.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.cpuis.utils.CsvExportUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test")
@Slf4j
public class Test {
    @GetMapping("/file")
    public void getTrainingPercentage(HttpServletResponse response) {
        try {

            // 构造导出数据结构
            String titles = "id1,id2";  // 设置表头
            String keys = "id1,id2";  // 设置每列字段

            // 构造导出数据
            List<Map<String, Object>> datas = new ArrayList<> ();
            Map<String, Object> map;


            map = new HashMap<> ();
            map.put ("id1", 1);
            map.put ("id2", 2);
            datas.add (map);

            // 设置导出文件前缀
            String fName = "test_";

            // 文件导出
            OutputStream os = response.getOutputStream ();
            CsvExportUtil.responseSetProperties (fName, response);
            CsvExportUtil.doExport (datas, titles, keys, os);
            os.close ();
        } catch (Exception e) {
            log.error ("", e);
        }
    }

}
