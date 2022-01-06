package edu.nwpu.cpuis.service;

import edu.nwpu.cpuis.entity.ErrCode;
import edu.nwpu.cpuis.entity.Response;
import edu.nwpu.cpuis.entity.exception.CpuisException;
import edu.nwpu.cpuis.entity.vo.PredictVO;
import edu.nwpu.cpuis.train.PythonScriptRunner;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class PredictionService {
    @Resource
    private TracedModelService service;

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

    private String getTempFileName(PredictVO vo, MultipartFile file) {
        return vo.getAlgoName () + "-" + vo.getDataset () + "-" + file.getOriginalFilename ();
    }

    private String adjust(String s) {
        return "\"" + s.trim () + "\"";
    }

}
