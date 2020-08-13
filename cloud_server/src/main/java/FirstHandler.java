import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import server.Callback;
import server.Const;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FirstHandler extends ChannelInboundHandlerAdapter {
    private ClientStatus clientStatus;
    private ByteBuf accumulator;
    public final int COMMAND_ACC_CAPACITY = 4;


    private byte[] bytes = new byte[10*1024*1024];


    public FirstHandler(){
        clientStatus = new ClientStatus();
        if(!Files.exists(Paths.get(Const.CLOUD_PACKAGE))) {
            try {
                Files.createDirectory(Paths.get(Const.CLOUD_PACKAGE));
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }
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

            //при закачке 35Гб файла, да и вообще любого свыше мегабайта. упираюсь в ограничение максимального пакета TCP/IP 65536,
            //то есть Server.FileStorageService.uploadFile вызывается на каждый макс пакет 65536, хотя accumulator может растягиваться до 10МБ
            // как будет эффективней, оставить как есть, или попытаться копить в аккумуляторе до 8-10МБ и только потом вызывать Server.FileStorageService.uploadFile ???
            // позволит ли это увеличить скорость закачки?

            FileStorageService.uploadFile(ctx, clientStatus, accumulator, bytes, new Callback() {
                @Override
                public void callback() {
                    System.out.println("file upload done " + clientStatus.getCurrentFileName());
                    clearStatusSetWaitCommand(ctx);
                    sendCommand(ctx, Command.commandOK);
                }
            });
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
            sendCommand(ctx, Command.commandOK);
        }
    }


    void setAccumulatorCapacity(ChannelHandlerContext ctx, int accumulatorCapacity, int maxCapacity){
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

    private void clearStatusSetWaitCommand(ChannelHandlerContext ctx){
        clientStatus.setCurrentFileSize(-1);
        clientStatus.setCurrentFileName(null);
        clientStatus.setCurrentAction(CurrentAction.COMMAND_SIZE);
        setAccumulatorCapacity(ctx, COMMAND_ACC_CAPACITY, COMMAND_ACC_CAPACITY);
    }
    private void sendCommand(ChannelHandlerContext ctx, byte command) {
        ByteBuf buf = null;
        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(command);
        ctx.writeAndFlush(buf);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(clientStatus.getLogin() + " отключился");
    }
}
