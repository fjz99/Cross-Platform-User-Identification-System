package site.cpuis.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import site.cpuis.entity.ErrCode;
import site.cpuis.entity.MongoOutputEntity;
import site.cpuis.entity.Response;
import site.cpuis.entity.exception.CpuisException;
import site.cpuis.entity.vo.ModelLocationVO;
import site.cpuis.entity.vo.OutputSearchVO;
import site.cpuis.entity.vo.PredictVO;
import site.cpuis.train.PythonScriptRunner;
import site.cpuis.utils.CsvExportUtil;
import site.cpuis.utils.ModelKeyGenerator;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PredictionService {
    @Resource
    protected DataBaseService<MongoOutputEntity> mongoOutputService;
    @Resource
    private TracedModelService service;
    @Resource
    private MatrixOutputModelService outputModelService;

    private boolean validateFile(File file) {
        if (file == null || !file.exists () || !file.isFile () || !file.canRead ()) {
            return false;
        }

        String name = file.getName ();
        return !StringUtils.isBlank (name) && name.endsWith (".txt");
    }


    public Object predict(PredictVO vo, MultipartFile file) throws IOException {

        if (vo.getDataset () == null || vo.getDataset ().size () != 1) {
            return Response.ofFailed (ErrCode.WRONG_DATASET_INPUT);
        }

        //验证数据集
        PythonScriptRunner.TracedScriptOutput output;
        String property = System.getProperty ("java.io.tmpdir");
        if (file != null) {
            File target = new File (property, getTempFileName (vo, file));
            file.transferTo (target);
            if (!validateFile (target)) {
                throw new CpuisException (ErrCode.WRONG_INPUT, "输入文件" + file.getName () + "验证失败");
            }
            Map<String, String> inputs = new HashMap<String, String> () {
                {
                    put ("file", adjust (target.getAbsolutePath ()));
                }
            };
            output = service.predict (vo, inputs);
        } else {
            Map<String, String> inputs = new HashMap<String, String> () {
                {
                    put ("input", adjust (vo.getInput ()));
                }
            };
            output = service.predict (vo, inputs);
        }
        return output.getOutput ();
    }

    public Object search(OutputSearchVO search) {
//        OutputSearchVO search = vo.getSearch ();
//        search.setPhase ("train");
//        search.setAlgoName (vo.getAlgoName ());
//        search.setDataset (vo.getDataset ().toArray (new String[0]));
        return outputModelService.getTracedOutput (search);
    }

    private String getTempFileName(PredictVO vo, MultipartFile file) {
        return vo.getAlgoName () + "-" + vo.getDataset () + "-" + file.getOriginalFilename ();
    }

    private String adjust(String s) {
        return "" + s.trim () + "";
    }

    public void download(ModelLocationVO vo, HttpServletResponse response) {
        try {
            String titles = String.join (",", vo.getDataset ());  // 设置表头
            String keys = "a,b";  // 设置每列字段
            List<Map<String, Object>> data = new ArrayList<> ();


            final String key = ModelKeyGenerator.generateKeyWithIncId (vo.getDataset (),
                    vo.getAlgoName (), "train", "output", 0, true);
            List<MongoOutputEntity> entities = mongoOutputService.selectAll (key, MongoOutputEntity.class);
            for (MongoOutputEntity entity : entities) {
                Map<String, Object> map = new HashMap<> ();
                map.put ("a", entity.getUserName ());
                if (entity.getOthers ().size () == 0)
                    continue;
                map.put ("b", entity.getOthers ().get (0).getUserName ());
//                entity.getOthers ().sort (Comparator.comparing (MongoOutputEntity.OtherUser::getSimilarity).reversed ());
//                if (isId) {
//                    map.put ("a", entity.getUserName ());
//                    map.put ("b", entity.getOthers ().get (0).getUserName ());
//                } else {
//
//                    map.put ("a", entity.getUserName ());
//                    map.put ("b", 2);
//                }

                data.add (map);
            }


            String fName = String.join ("_", vo.getDataset ()) + "_output";

            OutputStream os = response.getOutputStream ();
            CsvExportUtil.responseSetProperties (fName, response);
            CsvExportUtil.doExport (data, titles, keys, os);
            os.close ();
        } catch (Exception e) {
            log.error ("", e);
        }
    }

}
