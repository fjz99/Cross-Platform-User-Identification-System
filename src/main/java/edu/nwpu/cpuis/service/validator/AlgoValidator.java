package edu.nwpu.cpuis.service.validator;

import edu.nwpu.cpuis.entity.AlgoEntity;
import edu.nwpu.cpuis.entity.ErrCode;
import edu.nwpu.cpuis.entity.exception.CpuisException;
import org.springframework.stereotype.Component;


@Component
public class AlgoValidator {

    public void validate(Object target) {
        AlgoEntity entity = (AlgoEntity) target;

        if (entity.getName () == null) {
            throw new CpuisException (ErrCode.ALGO_VALIDATION_FAILED, "算法名不能为空");
        }

        if (entity.getStage () == null ||
                Integer.parseInt (entity.getStage ()) < 1 ||
                Integer.parseInt (entity.getStage ()) > 3){
            throw new CpuisException (ErrCode.ALGO_VALIDATION_FAILED, "算法阶段错误");
        }

            if (entity.getTrainSource () == null &&
                    entity.getPredictSource () == null &&
                    entity.getTestSource () == null) {

                throw new CpuisException (ErrCode.ALGO_VALIDATION_FAILED, "算法文件不能全为空");
            }
    }
}
