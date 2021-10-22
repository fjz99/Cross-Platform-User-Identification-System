package edu.nwpu.cpuis.utils;


import edu.nwpu.cpuis.entity.vo.OutputSearchVO;
import edu.nwpu.cpuis.service.DatasetService;
import edu.nwpu.cpuis.service.model.ModelDefinition;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.regex.Pattern;

@Component
public class OutputVoValidator implements Validator {
    private final DatasetService datasetService;
    private final ModelDefinition definition;

    public OutputVoValidator(DatasetService datasetService, ModelDefinition definition) {
        this.datasetService = datasetService;
        this.definition = definition;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return OutputSearchVO.class == clazz;
    }

    @Override
    public void validate(Object target, Errors errors) {
        OutputSearchVO searchVO = (OutputSearchVO) target;
        //dataset
        String[] dataset = searchVO.getDataset ();
        if (dataset == null || dataset.length > 2 || dataset.length < 1) {
            errors.rejectValue ("dataset", "0", "数据集输入错误");
        }
        for (String s : dataset) {
            if (datasetService.getDatasetLocation (s) == null) {
                errors.rejectValue ("dataset", "0", "数据集不存在");
            }
        }
        //algoName
        if (!definition.getDefinition ().containsKey (searchVO.getAlgoName ())) {
            errors.rejectValue ("algoName", "0", "算法不存在");
        }
        //page不校验
        //id and range
        if (searchVO.getRange () != null) {
            if (searchVO.getRange ().length != 2) {
                errors.rejectValue ("range", "0", "range错误");
            }
            if (searchVO.getId () != null) {
                errors.rejectValue ("range", "0", "range和id不可以同时存在");
            }
        }
        Pattern pattern = Pattern.compile ("train|test|predict|evaluate");
        if (searchVO.getPhase () == null || !pattern.matcher (searchVO.getPhase ()).matches ()) {
            errors.rejectValue ("range", "0", "phase输入错误，取值为train|test|predict|evaluate");
        }
    }
}
