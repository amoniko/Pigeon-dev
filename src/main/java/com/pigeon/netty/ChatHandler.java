package com.pigeon.netty;

import com.SpringUtil;
import com.enums.MsgActionEnum;
import com.pigeon.service.UserService;
import com.pigeon.utils.JsonUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * TextWebSocketFrame: frame carries message in netty.
 * The type specialize in tackling the message in netty
 */
public class ChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    // Record and manage every client channel, the code is conventional.
    public static ChannelGroup users = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * On client connects to the server
     * We get its channel and leave it to the ChannelGroup
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        users.add(ctx.channel());
    }


    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {

        String channelId = ctx.channel().id().asShortText();
        System.out.println("client removed, its short id is: " + channelId);

        // when handlerRemoved is called, ChannelGroup will remove its corresponding channel for us

        users.remove(ctx.channel());


    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        // close the channel if exception occurs. ChannelGroup will automatically remove client's channel.
        ctx.channel().close();
        users.remove(ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {

        // Get the message from client
        String content = msg.text();

        Channel currentChannel = ctx.channel();
        // 1.Get the message from client
        DataContent dataContent = JsonUtils.jsonToPojo(content, DataContent.class);
        Integer action = dataContent.getAction();
        // 2.Distinguish the message type with enums and deal with them respectively

        if(action == MsgActionEnum.CONNECT.type){
            // 2.1 initialize channel when WebSocket open first time, connect userid and channel.
            String senderId = dataContent.getChatMsg().getSenderId();
            UserChannelRel.put(senderId, currentChannel);

            // Test
            for(Channel  c : users){
                System.out.println(c.id().asLongText());
            }
            UserChannelRel.output();
        }

        else if (action == MsgActionEnum.CHAT.type){
            // 2.2 type of chat message, save them in the database.Mark message's status(received or not).
            ChatMsg chatMsg = dataContent.getChatMsg();
            String msgText = chatMsg.getMsg();
            String receiverId = chatMsg.getReceiverId();
            String senderId = chatMsg.getSenderId();

            // save the message to database and mark them as unread.
            UserService userService = (UserService) SpringUtil.getBean("userServiceImpl");
            String msgId = userService.saveMsg(chatMsg);
            chatMsg.setMsgId(msgId);

            DataContent dataContentMsg = new DataContent();
            dataContentMsg.setChatMsg(chatMsg);

            // send the message
            // get the channel from global Channel relationship
            Channel receiverChannel = UserChannelRel.get(receiverId);
            if(receiverChannel == null ){
                // channel empty, user offline. push the message
            } else{
                // channel not empty, check if the channel exists in ChannelGroup
                Channel findChannel = users.find(receiverChannel.id());
                if(findChannel != null){
                    // user online
                    receiverChannel.writeAndFlush
                            (new TextWebSocketFrame(JsonUtils.objectToJson(chatMsg)));
                }else{
                    // user offline. push the message
                }
            }
        }

        else if (action == MsgActionEnum.SIGNED.type){
            // 2.3 type of received message, alter the status record in the database.
            UserService userService = (UserService) SpringUtil.getBean("userServiceImpl");
            // Extend field represents the id of message pending to be signed, which is separate with comma.
            String msgIdsStr = dataContent.getExtand();
            String msgIds[]  = msgIdsStr.split(",");

            List<String> msgIdList = new ArrayList<>();
            for(String mid : msgIds){
                if(StringUtils.isNoneBlank(mid)){
                    msgIdList.add(mid);
                }
            }
            System.out.println(msgIdList.toString());

            if(msgIdList != null && !msgIdList.isEmpty() && msgIdList.size() > 0){
                // batch sign message
                userService.updateMsgSigned(msgIdList);
            }

        } else if (action == MsgActionEnum.KEEPALIVE.type){
            // 2.4 ping-pong status.
            System.out.println("messge received from channel:[" + currentChannel + "]" +
                    "'s ping pong message");
        }
    }
}

