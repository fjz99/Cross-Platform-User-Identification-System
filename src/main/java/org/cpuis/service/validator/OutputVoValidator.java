package org.cpuis.service.validator;


import org.cpuis.entity.vo.OutputSearchVO;
import org.cpuis.service.AlgoService;
import org.cpuis.service.DatasetService;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Objects;
import java.util.regex.Pattern;

@Component
public class OutputVoValidator implements Validator {
    private final DatasetService datasetService;
    private final AlgoService algoService;

    public OutputVoValidator(DatasetService datasetService,
                             AlgoService algoService) {
        this.datasetService = datasetService;
        this.algoService = algoService;
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
        if (!algoService.exists (searchVO.getAlgoName ())) {
            errors.rejectValue ("algoName", "0", "算法不存在");
        }
        //page不校验
        //id and range
        Pattern pattern = Pattern.compile ("train|test|predict|evaluate");
        if (searchVO.getPhase () == null || !pattern.matcher (searchVO.getPhase ()).matches ()) {
            errors.rejectValue ("range", "0", "phase输入错误，取值为train|test|predict|evaluate");
        }
        //type
        Pattern types = Pattern.compile ("output|statistics");
        if (searchVO.getType () == null || !types.matcher (searchVO.getType ()).matches ()) {
            errors.rejectValue ("range", "0", "type输入错误，取值为output|statistics");
        }
        //search type
        types = Pattern.compile ("fulltext|regex|normal|");
        if (searchVO.getSearchType () == null || Objects.equals (searchVO.getSearchType (), "")) {
            return;
        }
        if (!types.matcher (searchVO.getSearchType ()).matches ()) {
            errors.rejectValue ("searchType", "0", "search type输入错误，取值为output|statistics");
        }
    }
}
