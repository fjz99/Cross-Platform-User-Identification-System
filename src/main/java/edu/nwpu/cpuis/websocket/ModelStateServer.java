package edu.nwpu.cpuis.websocket;

import com.alibaba.fastjson.JSON;
import edu.nwpu.cpuis.entity.ModelTrainingInfo;
import edu.nwpu.cpuis.train.ModelTrainingInfoService;
import edu.nwpu.cpuis.utils.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * websocket和bean无关，一个请求一个class实例
 * 所以也没法注入bean
 * 只能通过appcontext去getBean
 */
@ServerEndpoint("/modelState/{id}")
@Slf4j
@Component("modelStateServer")
public class ModelStateServer {
    //注意static,原因未知，似乎是一个连接对应一个实例
    private static final Map<String, List<Session>> id2client = new ConcurrentHashMap<> ();

    //依赖不会注入，
//    @Autowired

    public static void changeState(@NonNull ModelTrainingInfo info) throws IOException {
        //发送给all
        List<Session> all = id2client.get ("all");
        if (all != null) {
            for (Session client : all) {
                client.getBasicRemote ().sendText (JSON.toJSONString (info));
            }
        }

        //..
        String id = info.getId ();
        List<Session> sessions = id2client.get (id);
        if (sessions == null) {
            log.warn ("没有client在监听 {},当前有监听的有 {}", id, id2client.keySet ());
            return;
        }
        for (Session client : sessions) {
            client.getBasicRemote ().sendText (JSON.toJSONString (info));
        }
    }

    //建立连接成功调用
    @OnOpen
    public void onOpen(Session session, @PathParam(value = "id") String id) {
        log.info ("{} client 开始监听 model {}", session.getId (), id);
        if (!id2client.containsKey (id)) {
            id2client.put (id, new ArrayList<> ());
        }
        id2client.get (id).add (session);

        try {
            sendInitialState (session, id);
        } catch (IOException e) {
            e.printStackTrace ();
        }
    }

    private void sendInitialState(Session session, String id) throws IOException {
        //发送初始状态
        ModelTrainingInfoService service = SpringContextHolder.getBean ("modelTrainingInfoService");
        if (id.equals ("all")) {
            List<ModelTrainingInfo> models = service.getModels ();
            session.getBasicRemote ().sendText (JSON.toJSONString (models));
            return;
        }
        ModelTrainingInfo info = service.getInfo (id);
        if (info == null) {
            info = ModelTrainingInfo.builder ()
                    .id (id).message ("模型不存在").build ();
        }
        session.getBasicRemote ().sendText (JSON.toJSONString (info));
    }

    @OnClose
    public void onClose(Session session, @PathParam(value = "id") String id) {
        log.info ("{} client 停止监听 model {}", session.getId (), id);
        id2client.get (id).remove (session);
    }

    //收到客户端信息
    @OnMessage
    public void onMessage(Session session, String message) throws IOException {
        log.error ("{} client send msg {}", session.getId (), message);
    }
}
