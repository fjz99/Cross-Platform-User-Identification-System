package edu.nwpu.cpuis.service;

import edu.nwpu.cpuis.entity.MongoEntity;
import edu.nwpu.cpuis.entity.Output;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * @author fujiazheng
 * @see edu.nwpu.cpuis.service.AbstractModelService
 */
@Service
@SuppressWarnings("unchecked")
//TODO 增加 getAllReversedMatchedUsers方法
public class MatrixOutputModelService extends AbstractModelService {

    public @Nullable
    Object getMatchedUsers(@NonNull String userId, @NonNull String key, boolean reverse) throws ClassCastException {
        Output output = mongoService.selectById (key, MongoEntity.class, "output").getOutput ();
        Map<String, Object> output1 = (Map<String, Object>) output.getOutput ();
        //fixme 因为output是一个Object，所以自动解析为了一个key是String的map，然后因为类型擦除导致。。类型转换成功
        if (reverse) {
            return getReversedMatchedUsers (output1, userId);
        }
        return output1.get (userId);
    }

    //返回list[list]
    private Object getReversedMatchedUsers(Map<String, Object> output, String userId) {
        List<Object> list = new ArrayList<> ();
        output.forEach ((k, v) -> {
            ((List<Object>) v).forEach (x -> {
                List<Object> temp = ((List<Object>) x);
                String id = String.valueOf (temp.get (0));
                if (id.equals (userId)) {
                    List<Object> r = new ArrayList<> ();
                    r.add (k);
                    r.add (temp.get (1));
                    list.add (r);
                }
            });
        });
        return list;
    }

    public @Nullable
    Object getAllMatchedUsers(@NonNull String key) throws ClassCastException {
        Output output = mongoService.selectById (key, MongoEntity.class, "output").getOutput ();
        return output.getOutput ();
    }
}
