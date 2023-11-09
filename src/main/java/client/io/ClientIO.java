package client.io;

import com.alibaba.fastjson.JSON;
import common.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.unix.PreferredDirectByteBufAllocator;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ClientIO {

    private AtomicLong REQUEST_ID_GENERATOR = new AtomicLong(0);

    private Bootstrap clientBoot;

    private Channel serverChannel;

    private Map<Long, RpcRequest> pendingRequests = new ConcurrentHashMap<>();
    private Map<Long, CompletableFuture<RpcResponse>> callbackHolder = new ConcurrentHashMap<>();

    public void startAndConnect() throws InterruptedException {

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

        this.clientBoot = new Bootstrap();
        this.clientBoot.group(workerGroup);
        if (Epoll.isAvailable()) {
            this.clientBoot.channel(EpollSocketChannel.class);
        } else if (KQueue.isAvailable()) {
            this.clientBoot.channel(KQueueSocketChannel.class);
        } else {
            this.clientBoot.channel(NioSocketChannel.class);
        }

        this.clientBoot.option(ChannelOption.ALLOCATOR, PreferredDirectByteBufAllocator.DEFAULT)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 600000)
                .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1024 * 1024, 4 * 1024 * 1024))
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_SNDBUF, 32 * 1024 * 1024)
                .option(ChannelOption.SO_RCVBUF, 32 * 1024 * 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_KEEPALIVE, true);

        this.clientBoot.handler(new ChannelInitializer<Channel>() {
            @Override
            public void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,0,4));
                ch.pipeline().addLast(new ProtoDecoder());
                ch.pipeline().addLast(new LengthFieldPrepender(4));
                ch.pipeline().addLast(new ProtoEncoder());
                ch.pipeline().addLast(new ClientIOHandler());
            }
        });

        SocketAddress address = new InetSocketAddress(9090);
        this.serverChannel = this.clientBoot.connect(address).sync().channel();
    }

    public CompletableFuture<RpcResponse> sendRpcRequest(RpcRequest request) {
        long requestId = REQUEST_ID_GENERATOR.incrementAndGet();
        TransportProtocol protocol = new TransportProtocol();
        protocol.setRequestId(requestId);
        protocol.setMessageType(MessageType.RPC_REQ);

        // 还是JSON
        protocol.setBody(JSON.toJSONString(request));

        pendingRequests.put(requestId, request);

        CompletableFuture<RpcResponse> future = new CompletableFuture<>();
        callbackHolder.put(requestId, future);

        this.serverChannel.writeAndFlush(protocol);

        return future;
    }


    public class ClientIOHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            TransportProtocol protocol = (TransportProtocol) msg;

            long requestId = protocol.getRequestId();
            int messageType = protocol.getMessageType();

            if (messageType == MessageType.RPC_RESP) {


                RpcRequest request = pendingRequests.remove(requestId);
                CompletableFuture<RpcResponse> future = callbackHolder.remove(requestId);
                RpcResponse response = JSON.parseObject(protocol.getBody(), RpcResponse.class);
                // 服务端不回写异常，这里也不处理,简单点
                future.complete(response);
            }

            // TODO: 2023/11/8 该干点啥干点啥吧

        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("connected to server, local = " + ctx.channel().localAddress() + ", remote = " + ctx.channel().remoteAddress());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            Channel channel = ctx.channel();
            System.out.println("disconnect from server :" + channel.remoteAddress());
        }

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
            System.out.println("write changed");
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
        }

    }


}
