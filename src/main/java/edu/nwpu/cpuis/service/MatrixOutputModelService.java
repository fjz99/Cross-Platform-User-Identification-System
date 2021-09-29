package edu.nwpu.cpuis.service;

import edu.nwpu.cpuis.entity.MongoEntity;
import edu.nwpu.cpuis.entity.Output;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Service
@SuppressWarnings("unchecked")
public class MatrixOutputModelService extends AbstractModelService {
    public @Nullable
    Object getMatchedUsers(@NonNull String userId, @NonNull String key) throws ClassCastException {
        Output output = mongoService.selectById (key, MongoEntity.class, "output").getOutput ();
        Map<String, Object> output1 = (Map<String, Object>) output.getOutput ();
        //fixme 因为output是一个Object，所以自动解析为了一个key是String的map，然后因为类型擦除导致。。类型转换成功
        return output1.get (userId);
    }

    public @Nullable
    Object getAllMatchedUsers(@NonNull String key) throws ClassCastException {
        Output output = mongoService.selectById (key, MongoEntity.class, "output").getOutput ();
        return output.getOutput ();
    }
}
