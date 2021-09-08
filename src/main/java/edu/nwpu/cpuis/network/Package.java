package edu.nwpu.cpuis.network;

import com.alibaba.fastjson.annotation.JSONField;
import edu.nwpu.cpuis.network.OperationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 报文实体类,看起来像rpc
 *
 * @author fujiazheng
 * @see OperationType
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Package {
    private int sessionId;
    private String name;//可以是空，因为有sessionId
    private OperationType type;
    private Map<String, Object> args;

    @JSONField(name = "type")
    public int getCode() {
        return type.getCode ();
    }
}
