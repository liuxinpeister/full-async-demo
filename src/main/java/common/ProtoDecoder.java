package common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class ProtoDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        in.skipBytes(4);
        TransportProtocol protocol = new TransportProtocol();
        protocol.setRequestId(in.readLong());
        protocol.setMessageType(in.readInt());
        int bodyLength = in.readInt();
        byte[] bodyBytes = new byte[bodyLength];
        in.readBytes(bodyBytes,0, bodyLength);
        protocol.setBody(new String(bodyBytes, StandardCharsets.UTF_8));

        out.add(protocol);
    }
}
