import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FirstHandler extends ChannelInboundHandlerAdapter {
    private ClientStatus clientStatus;
    private ByteBuf accumulator;
    public final int COMMAND_ACC_CAPACITY = 4;

    public FirstHandler(){
        clientStatus = new ClientStatus();
    }

    public ClientStatus getClientStatus() {
        return clientStatus;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        ByteBufAllocator allocator = ctx.alloc();
        accumulator = allocator.directBuffer(COMMAND_ACC_CAPACITY, COMMAND_ACC_CAPACITY);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf inBuf = (ByteBuf) msg;
        accumulator.writeBytes(inBuf);
        inBuf.release();
        if(clientStatus.getCurrentAction() == CurrentAction.UPLOAD){
            ctx.fireChannelRead(accumulator);
            return;
        }
        if (accumulator.readableBytes() == accumulator.capacity() && clientStatus.getCurrentAction() != CurrentAction.COMMAND_SIZE)
        {
            ctx.fireChannelRead(accumulator);
        }
        else if(accumulator.readableBytes() == accumulator.capacity())
        {
            if(clientStatus.getCurrentAction() == CurrentAction.COMMAND_SIZE ){clientStatus.setCurrentAction(CurrentAction.WAIT);}
            int cap = accumulator.readInt();
            setAccumulatorCapacity(ctx, cap, cap);
        }
    }


    public void setAccumulatorCapacity(ChannelHandlerContext ctx, int accumulatorCapacity, int maxCapacity){
        accumulator.release();
        ByteBufAllocator allocator = ctx.alloc();
        accumulator = allocator.directBuffer(accumulatorCapacity, maxCapacity);
        System.out.println("capacity set = " + accumulator.capacity());
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    public static class FileStorageService {
        private static final String clientServer = "./cloudserver_logins";

        public static void sendToClientFile(ChannelHandlerContext ctx, ClientStatus clientStatus, ChannelFutureListener finishListener) throws IOException {
                ctx.pipeline().remove(OutBytesToByteBufHandler.class);
                Path path = Paths.get(clientServer + "/" + clientStatus.getLogin() + "/" + clientStatus.getCurrentFileName());
                FileRegion region = new DefaultFileRegion(path.toFile(), 0, Files.size(path));
                ChannelFuture future = ctx.channel().writeAndFlush(region);
                if (finishListener != null) {
                    future.addListener(finishListener);
                }
                ctx.pipeline().addFirst(new OutBytesToByteBufHandler());
                //стоит ли вообще заморачиваться с этим хандлером может делать какой-нибудь аллокейт на 1 байт и посылать оккоманды?

        }

        public static void sendStorageInfo(){}
    }
}
