package edu.nwpu.cpuis.websocket;

import com.alibaba.fastjson.JSON;
import edu.nwpu.cpuis.entity.ModelTrainingInfo;
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

@ServerEndpoint("/modelState/{id}")
@Component
@Slf4j
public class ModelStateServer {
    private final Map<String, String> client2id = new ConcurrentHashMap<> ();
    private final Map<String, List<Session>> id2client = new ConcurrentHashMap<> ();

    //建立连接成功调用
    @OnOpen
    public void onOpen(Session session, @PathParam(value = "id") String id) {
        log.info ("{} client 开始监听 model {}", session.getId (), id);
        client2id.put (session.getId (), id);
        id2client.computeIfAbsent (id, x -> new ArrayList<> ()).add (session);
    }

    @OnClose
    public void onClose(Session session, @PathParam(value = "id") String id) {
        log.info ("{} client 停止监听 model {}", session.getId (), id);
        client2id.remove (session.getId ());
        id2client.get (id).remove (session);
    }

    //收到客户端信息
    @OnMessage
    public void onMessage(Session session, String message) throws IOException {
        log.error ("{} client send msg {}", session.getId (), message);
    }

    public void changeState(@NonNull ModelTrainingInfo info) throws IOException {
        String id = info.getId ();
        for (Session client : id2client.get (id)) {
            client.getBasicRemote ().sendText (JSON.toJSONString (info));
        }
    }
}
