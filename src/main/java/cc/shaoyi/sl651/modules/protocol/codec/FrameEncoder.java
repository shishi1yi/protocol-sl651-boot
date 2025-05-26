package cc.shaoyi.sl651.modules.protocol.codec;


import cc.shaoyi.sl651.modules.protocol.entity.HexFrameHeaderMessage;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameWrapper;
import io.netty.buffer.ByteBuf;

public interface FrameEncoder {

	ByteBuf encodeReply(HexFrameWrapper frameWrapper);

	ByteBuf encodeAsk(HexFrameHeaderMessage headerMessage);
}
