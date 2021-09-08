package edu.nwpu.cpuis.network;

import com.alibaba.fastjson.JSONObject;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;

/**
 * 封装了和python进行通信的逻辑 <br/>
 * 一个请求一个响应，如果要实现进度条需要多次请求进度
 *
 * @author fujiazheng
 * @see edu.nwpu.cpuis.network.ModelClient
 */
@Slf4j
public class SocketModelClient implements ModelClient {
    @Value ("${socket.host}")
    private String host;
    @Value ("${socket.port}")
    private int port;
    private EventLoopGroup group;
    private ChannelFuture future;

    public SocketModelClient() {
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        group.shutdownGracefully ().sync ();
        log.info ("SocketModelClient {}:{} shutdown", host, port);
    }

    @PostConstruct
    public void start() throws Exception {
        group = new NioEventLoopGroup ();
        Bootstrap b = new Bootstrap ();
        b.group (group)
                .channel (NioSocketChannel.class)
                .remoteAddress (new InetSocketAddress (host, port))
                .handler (new ChannelInitializer<SocketChannel> () {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        log.debug ("initChannel ch:" + ch);
                        ch.pipeline ()
                                .addLast ("decoder", new DelimiterBasedFrameDecoder (Integer.MAX_VALUE, Delimiters.lineDelimiter ()[1]))
                                .addLast ("encoder", new StringEncoder ())
                                .addLast ("handler", new ModelClientHandler ());
                    }
                });
        future = b.connect ().sync ();
        log.info ("SocketModelClient init for {}:{}", host, port);
    }

    @Override
    public Package send(Package req) throws InterruptedException {
        future.channel ().writeAndFlush (JSONObject.toJSONString (req) + "\n");
        //当通道关闭了，就继续往下走
        future.channel ().closeFuture ().sync ();
        //接收服务端返回的数据
        AttributeKey<String> key = AttributeKey.valueOf ("ServerData");
        String result = future.channel ().attr (key).get ();
        log.info (result);
        return JSONObject.parseObject (result, Package.class);
    }

    @ChannelHandler.Sharable
    //指的是不会冲突，可以多次添加的handler
    private static class ModelClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

        @Override
        public void channelRead0(ChannelHandlerContext ctx,
                                 ByteBuf in) {
            AttributeKey<String> key = AttributeKey.valueOf ("ServerData");
            ctx.channel ().attr (key).set (in.toString (CharsetUtil.UTF_8));
            //把客户端的通道关闭
            ctx.channel ().close ();

            log.debug ("Client received: " + in.toString (CharsetUtil.UTF_8));
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx,
                                    Throwable cause) {
            cause.printStackTrace ();
            ctx.close ();
            log.error (cause.toString ());
        }
    }

}
