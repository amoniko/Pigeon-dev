package com.pigeon.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 *  Detect channel's ping-pong event.
 *
 */
public class HeartBeatHandler extends ChannelInboundHandlerAdapter{

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt)  {

        //trigger events of write idle, read idle or both.
        if (evt instanceof IdleStateEvent){
                IdleStateEvent event = (IdleStateEvent)evt;

                if(event.state() == IdleState.READER_IDLE){
//                    System.out.println("read idle");
                } else if (event.state() == IdleState.WRITER_IDLE){
//                    System.out.println("write idle");
                } else if (event.state() == IdleState.ALL_IDLE){
                    Channel channel = ctx.channel();
                    // shut down unused channel to release the resources.
                    channel.close();

//                    System.out.println("asdas" + ChatHandler.users.size());
                }
        }
    }
}

