package server.io;

import com.alibaba.fastjson.JSON;
import common.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.unix.PreferredDirectByteBufAllocator;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import server.contract.IUserCenterRpcService;
import server.contract.UserInfo;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class ServerIO {

    private IUserCenterRpcService app;

    private ChannelFuture channelFuture;

    public void startAndWait(IUserCenterRpcService app) {

        this.app = app;

        // 其实这里可以自动扫描，生成代理类，这里直接传进来，demo嘛，简单点

        EventLoopGroup bossGroup = null;
        EventLoopGroup workerGroup = null;

        if (Epoll.isAvailable()) {
            bossGroup = new EpollEventLoopGroup(1);
            workerGroup = new EpollEventLoopGroup(1);
        } else if (KQueue.isAvailable()) {
            bossGroup = new KQueueEventLoopGroup(1);
            workerGroup = new KQueueEventLoopGroup(1);
        } else {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup(1);
        }


        try {
            ServerBootstrap bootstrap = new ServerBootstrap();

            bootstrap.group(bossGroup, workerGroup)
                    .childOption(ChannelOption.ALLOCATOR, PreferredDirectByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1024 * 1024, 4 * 1024 * 1024))
                    .childOption(ChannelOption.SO_SNDBUF, 8 * 1024 * 1024)
                    .childOption(ChannelOption.SO_RCVBUF, 8 * 1024 * 1024);

            if (Epoll.isAvailable()) {
                bootstrap.channel(EpollServerSocketChannel.class).option(ChannelOption.SO_REUSEADDR, true);
            } else if (KQueue.isAvailable()) {
                bootstrap.channel(KQueueServerSocketChannel.class).option(ChannelOption.SO_REUSEADDR, true);
            }else {
                bootstrap.channel(NioServerSocketChannel.class).option(ChannelOption.SO_REUSEADDR, true);
            }

            bootstrap.childHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel channel) throws Exception {
                    channel.pipeline()
                            .addLast(new LengthFieldPrepender(4))
                            .addLast(new ProtoEncoder())
                            .addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4))
                            .addLast(new ProtoDecoder())
                            .addLast(new ServerIOHandler());
                }
            });

            SocketAddress address = new InetSocketAddress(9090);
            this.channelFuture = bootstrap.bind(address);
            this.channelFuture.addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    System.out.println("server start success");
                }
            });



        } catch (Exception e) {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }

    }


    public class ServerIOHandler extends ChannelInboundHandlerAdapter {

        // 这个map其实不是必须，只是预留为了演示其他实现形式，比如58同城中间件的异步回写是这么做的
        private Map<Long, RpcRequest> requests = new ConcurrentHashMap<Long, RpcRequest>();

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("client connected " + ctx.channel().remoteAddress());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("client disconnect " + ctx.channel().remoteAddress());
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg == null) {
                return;
            }
            Channel channel = ctx.channel();
            TransportProtocol protocol = (TransportProtocol) msg;
            try {
                long requestId = protocol.getRequestId();
                int messageType = protocol.getMessageType();
                if(messageType == MessageType.RPC_REQ) {
                    // 这里为了简单，暂时用JSON充当序列化协议了，demo嘛
                    RpcRequest request = JSON.parseObject(protocol.getBody(), RpcRequest.class);
                    requests.put(requestId, request);
                    handleRpcRequest(requestId, channel, request);
                }

                // TODO: 其他逻辑先不写了，这里应该做点该做的的事儿

            } catch (Exception e) {
                // TODO: 其他逻辑先不写了，这里应该做点该做的的事儿，回写异常啥的
                e.printStackTrace();
            }

        }

        private void handleRpcRequest(long requestId, Channel clientChannel, RpcRequest request) {
            String service = request.getService();
            String method = request.getMethod();
            String param = request.getParam();

            // 这里假装模拟一下代理类的逻辑
            if("IUserCenterRpcService".equals(service) && method.equals("queryUserinfo")) {
                CompletableFuture<UserInfo> future = app.queryUserinfo(param);
                future.whenComplete(new BiConsumer<UserInfo, Throwable>() {
                    @Override
                    public void accept(UserInfo result, Throwable throwable) {
                        if(throwable != null) {
                            // 简单起见，这里就不设计异常的回写了，先不处理。demo嘛
                            return;
                        }


                        RpcResponse response = new RpcResponse();
                        response.setService(service);
                        response.setMethod(method);

                        // 用JSON来充当序列化协议吧
                        response.setResult(JSON.toJSONString(result));

                        TransportProtocol protocol = new TransportProtocol();
                        protocol.setBody(JSON.toJSONString(response));
                        protocol.setRequestId(requestId);
                        protocol.setMessageType(MessageType.RPC_RESP);

                        clientChannel.writeAndFlush(protocol);
                    }
                });
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
        }
    }

}
