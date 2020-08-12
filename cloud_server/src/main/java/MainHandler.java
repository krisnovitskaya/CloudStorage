import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import server.Callback;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainHandler extends ChannelInboundHandlerAdapter {
    private ClientStatus clientStatus;
    private final String clientServer = "cloudserver_logins";
    private byte[] buf = new byte[256];



    public MainHandler(ClientStatus clientStatus){
        this.clientStatus = clientStatus;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf accumulator = (ByteBuf) msg;
        //if(clientStatus.getCurrentAction() == CurrentAction.WAIT){
        byte signalByte = accumulator.readByte();
        if(signalByte == Command.upload){
            clientStatus.setCurrentAction(CurrentAction.UPLOAD);
            int filenamesize = accumulator.readInt();
            //int readableBytes = accumulator.readableBytes();
            accumulator.readBytes(buf, 0, filenamesize);
            clientStatus.setCurrentFileName(new String(buf, 0, filenamesize , StandardCharsets.UTF_8));
            //System.out.println(clientStatus.getCurrentFileName());
            Path path = Paths.get(clientServer + "/" + clientStatus.getLogin() + "/" + clientStatus.getCurrentFileName());
            Files.deleteIfExists(path);
            long filesize = accumulator.readLong();
            clientStatus.setCurrentFileSize(filesize);
            //System.out.println("filesize = " + filesize);
            accumulator.clear();
            ctx.pipeline().get(FirstHandler.class).setAccumulatorCapacity(ctx,1024, 10 * 1024 * 1024);
            sendCommand(ctx, Command.commandOK);
            return;
        }
        if(signalByte == Command.download){
            clientStatus.setCurrentAction(CurrentAction.DOWNLOAD);
            int filenamesize = accumulator.readInt();
            //System.out.println("filenamesize = " + filenamesize);
            accumulator.readBytes(buf, 0, filenamesize);
            clientStatus.setCurrentFileName(new String(buf, 0, filenamesize, StandardCharsets.UTF_8));
            //System.out.println(clientStatus.getCurrentFileName());
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
            //clientStatus.setCurrentAction(CurrentAction.STORAGE_INFO);
            FileStorageService.sendStorageInfo(ctx, clientStatus, new Callback() {
                @Override
                public void callback() {
                    System.out.println("storage info send");
                    clearStatusSetWaitCommand(ctx);
                }
            } );
            return;
        }

        if(signalByte == Command.delete_FILE){
            //clientStatus.setCurrentAction(CurrentAction.STORAGE_INFO);
            int filenamesize = accumulator.readInt();
            //System.out.println("filenamesize = " + filenamesize);
            int readableBytes = accumulator.readableBytes();
            //System.out.println("readablebytes" + readableBytes);
            accumulator.readBytes(buf, 0, filenamesize);
            accumulator.clear();
            clientStatus.setCurrentFileName(new String(buf, 0, filenamesize , StandardCharsets.UTF_8));
            //System.out.println(clientStatus.getCurrentFileName());
            //System.out.println(clientStatus.getCurrentFileName());
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
