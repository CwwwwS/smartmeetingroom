package cn.hephaestus.smartmeetingroom.websocket;

import cn.hephaestus.smartmeetingroom.model.UserInfo;
import cn.hephaestus.smartmeetingroom.service.RedisService;
import cn.hephaestus.smartmeetingroom.service.ServiceImpl.UserServiceImpl;
import cn.hephaestus.smartmeetingroom.service.UserService;
import cn.hephaestus.smartmeetingroom.utils.JwtUtils;
import cn.hephaestus.smartmeetingroom.utils.SpringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;


@ServerEndpoint("/getMessageServer/{token}")
@Component
public class MessageSocketServer {

    @Autowired
    RedisService redisService;

    private static HashMap<Integer,MessageSocketServer> webSocketMap = new HashMap<>();
    private static HashMap<Integer,List<String>> waitToSent=new HashMap<>();

    private Session session;
    private Integer id;
    private UserInfo userInfo;

    //发送给所有的用户
    public static void sentAll(String message){
        Set<Integer> set=webSocketMap.keySet();
        MessageSocketServer messageSocketServer=null;
        for (Integer it:set){
            messageSocketServer=webSocketMap.get(it);
            messageSocketServer.session.getAsyncRemote().sendText(message);
        }
    }

    //发送给指定的用户集合
    public static void sentAll(Integer[] idArr,String message){
        Session session=null;
        MessageSocketServer messageSocketServer=null;
        for (int i=0;i<idArr.length;i++){
            messageSocketServer=webSocketMap.get(idArr[i]);
            if (messageSocketServer==null){
                //如果当前用户不在线，则放在缓存列表中。。。。。。
                List<String> list=waitToSent.get(idArr[i]);
                if (list==null){
                    waitToSent.put(idArr[i],new LinkedList<>());
                }else {
                    list.add(message);
                }
                continue;
            }
            messageSocketServer.session.getAsyncRemote().sendText(message);
        }
    }


    //接受新的连接，并判断权限是否足够
    @OnOpen
    public void onOpen(Session session,@PathParam("token") String token) throws IOException {
        this.session=session;
        Integer id=JwtUtils.getId(token);
        if (id!=null){
            this.id=id;
            webSocketMap.put(id,this);//有新的连接，加入map中
            //判断又没有该用户的信息，如果有就发送
            List<String> list=waitToSent.get(id);//获取信息
            String message;
            if (list!=null){
                for (int i=0;i<list.size();i++){
                    message=list.get(i);
                    if (message!=null){
                        session.getAsyncRemote().sendText(message);
                    }
                    list.remove(i);
                }
            }
            System.out.println("有新的连接"+id);
        }else {
            session.close();
        }
    }

    @OnClose
    public void onClose() throws IOException {
        webSocketMap.remove(this.id);//连接断开，移除session
        this.session.close();
        System.out.println("一个连接断开"+this.id);
    }

    //接受来自客户端的消息，服务器起转发作用
    @OnMessage
    public void onMessage(String message) throws IOException {
        UserService userService= SpringUtil.getBean(UserServiceImpl.class);
        ObjectMapper objectMapper=new ObjectMapper();
        Message m=null;

        try {
            m=objectMapper.readValue(message,Message.class);
            m.getReciveId();
        }catch (Exception e){
            return;
        }

        if (m.getType().equals(Message.PERSON)){//个人对个人
            webSocketMap.get(m.getReciveId()).session.getAsyncRemote().sendText(m.toString());
        }else if (m.getType().equals(Message.MEETING)){
            Set<String> set=redisService.sget(m.getReciveId().toString());
            List<Integer> list=new LinkedList<>();
            for (String s:set){
                list.add(Integer.parseInt(s));
            }
            Integer[] idArray= (Integer[]) list.toArray();
            sentAll(idArray,message);
        }else if (m.getType().equals(Message.DEPARTMENT)){//发送给整个部门
            if (userInfo==null){
                userInfo=userService.getUserInfo(id);
            }
            Integer[] integers=userService.getAllUserByDeparment(userInfo.getOid(),userInfo.getDid());
            sentAll(integers,message);//发送
        }
        System.out.println("发送了消息"+m.toString());
    }

    //发送消息
    public void sendMessage(Integer id,String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }
}

/**
 * 定义一条消息
 */
@Setter
@Getter
@AllArgsConstructor
class Message{
    public Message() {
    }
    public static final String MEETING="meeting";
    public static final String DEPARTMENT="department";
    public static final String PERSON="person";
    private String type;//种类

    private Integer sentId;//发送者id

    private Integer reciveId;//接受者id

    public String content;//内容

    public long time;//发送时间

    public String toString(){
        ObjectMapper objectMapper=new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(this);
        }catch (Exception e){
            e.printStackTrace();
        }
        return "{}";
    }
}
