package common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;

public class ProtoEncoder extends MessageToByteEncoder<TransportProtocol> {


    protected void encode(ChannelHandlerContext channelHandlerContext, TransportProtocol transportProtocol, ByteBuf byteBuf) throws Exception {
        byteBuf.writeLong(transportProtocol.getRequestId());
        byteBuf.writeInt(transportProtocol.getMessageType());
        String body = transportProtocol.getBody();
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        byteBuf.writeInt(bytes.length);
        byteBuf.writeBytes(bytes);
    }

}
