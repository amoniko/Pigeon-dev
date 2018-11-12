package com.pigeon.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.time.LocalDateTime;

/**
 * TextWebSocketFrame: frame carries message in netty.
 * The type specialize in tackling the message in netty
 */
public class ChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    // Record and manage every client channel, the code is conventional.
    private static ChannelGroup clients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * On client connects to the server
     * We get its channel and leave it to the ChannelGroup
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        clients.add(ctx.channel());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        // when handlerRemoved is called, ChannelGroup will remove its corresponding channel for us

//        clients.remove(ctx.channel());
        System.out.println("Client disconnected, its long id is : "
                + ctx.channel().id().asLongText());
        System.out.println("Client disconnected, its short id is : "
                + ctx.channel().id().asShortText());

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {

        // Get the message from client
        String content = msg.text();

        System.out.println("message received: " + content);

        for(Channel channel: clients){
            channel.writeAndFlush
                    (new TextWebSocketFrame("[At " +
                            LocalDateTime.now() + " Server received message:] " + content)) ;
        }

        // The code below works the same as above.
        /*     clients.writeAndFlush("[At " +
                LocalDateTime.now() + " Server received message: " + content);*/
    }
}

