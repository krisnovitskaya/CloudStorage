import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import server.Callback;
import server.Const;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainHandler extends ChannelInboundHandlerAdapter {
    private ClientStatus clientStatus;
    private byte[] buf = new byte[256];



    public MainHandler(ClientStatus clientStatus){
        this.clientStatus = clientStatus;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf accumulator = (ByteBuf) msg;

        byte signalByte = accumulator.readByte();

        if(signalByte == Command.upload){
            clientStatus.setCurrentAction(CurrentAction.UPLOAD);
            int filenamesize = accumulator.readInt();

            accumulator.readBytes(buf, 0, filenamesize);
            clientStatus.setCurrentFileName(new String(buf, 0, filenamesize , StandardCharsets.UTF_8));

            Files.deleteIfExists(Paths.get(Const.CLOUD_PACKAGE + "/" + clientStatus.getLogin() + "/" + clientStatus.getCurrentFileName()));
            long filesize = accumulator.readLong();
            clientStatus.setCurrentFileSize(filesize);
            accumulator.clear();
            ctx.pipeline().get(FirstHandler.class).setAccumulatorCapacity(ctx,1024, 10 * 1024 * 1024);
            sendCommand(ctx, Command.commandOK);
            return;
        }

        if(signalByte == Command.download){
            clientStatus.setCurrentAction(CurrentAction.DOWNLOAD);
            int filenamesize = accumulator.readInt();
            accumulator.readBytes(buf, 0, filenamesize);
            clientStatus.setCurrentFileName(new String(buf, 0, filenamesize, StandardCharsets.UTF_8));

            System.out.println("start download");
            FileStorageService.sendToClientFile(ctx, clientStatus, futureListener ->{
                if (futureListener.isSuccess()) {
                    System.out.println("file send complete");
                } else {
                    futureListener.cause().printStackTrace();
                    System.out.println("download error");
                }
            });
            clearStatusSetWaitCommand(ctx);
            return;
        }

        if(signalByte == Command.storage_INFO){
            int storagePathSize = accumulator.readInt();
            accumulator.readBytes(buf, 0, storagePathSize);
            accumulator.clear();
            FileStorageService.sendStorageInfo(clientStatus, ctx, new String(buf, 0, storagePathSize, StandardCharsets.UTF_8), new Callback() {
                @Override
                public void callback() {
                    System.out.println("storage info send");
                    clearStatusSetWaitCommand(ctx);
                }
            } );
            return;
        }

        if(signalByte == Command.delete_FILE){
            int filenamesize = accumulator.readInt();
            accumulator.readBytes(buf, 0, filenamesize);
            accumulator.clear();
            clientStatus.setCurrentFileName(new String(buf, 0, filenamesize , StandardCharsets.UTF_8));

            FileStorageService.deleteFile(clientStatus, new Callback() {
                @Override
                public void callback() {
                    System.out.println("file deleted");
                    sendCommand(ctx, Command.commandOK);
                    clearStatusSetWaitCommand(ctx);
                }
            } );
        }


    }


    private void clearStatusSetWaitCommand(ChannelHandlerContext ctx){
        clientStatus.setCurrentFileSize(-1);
        clientStatus.setCurrentFileName(null);
        clientStatus.setCurrentAction(CurrentAction.COMMAND_SIZE);
        ctx.pipeline().get(FirstHandler.class).setAccumulatorCapacity(ctx,ctx.pipeline().get(FirstHandler.class).COMMAND_ACC_CAPACITY, ctx.pipeline().get(FirstHandler.class).COMMAND_ACC_CAPACITY);
    }
    private void sendCommand(ChannelHandlerContext ctx, byte command) {
        ByteBuf buf = null;
        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(command);
        ctx.writeAndFlush(buf);
    }
}
