import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainHandler extends ChannelInboundHandlerAdapter {
    private ClientStatus clientStatus;
    private final String clientServer = "./cloudserver_logins";


    public MainHandler(ClientStatus clientStatus){
        this.clientStatus = clientStatus;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf accumulator = (ByteBuf) msg;
        if(clientStatus.getCurrentAction() == CurrentAction.WAIT){
            byte signalByte = accumulator.readByte();
            if(signalByte == Command.upload){
                clientStatus.setCurrentAction(CurrentAction.UPLOAD);
                int filenamesize = accumulator.readInt();
                System.out.println("filenamesize = " + filenamesize);
                byte[] buf = new byte[filenamesize];
                accumulator.readBytes(buf);
                clientStatus.setCurrentFileName(new String(buf, StandardCharsets.UTF_8));
                System.out.println(clientStatus.getCurrentFileName());
                Path path = Paths.get(clientServer + "/" + clientStatus.getLogin() + "/" + clientStatus.getCurrentFileName());
                Files.deleteIfExists(path);
                long filesize = accumulator.readLong();
                clientStatus.setCurrentFileSize(filesize);
                System.out.println("filesize = " + filesize);
                accumulator.clear();
                ctx.pipeline().get(FirstHandler.class).setAccumulatorCapacity(ctx,1024, 5 * 1024 * 1024);
                sendCommand(ctx, Command.commandOK);
            }
            if(signalByte == Command.download){
                clientStatus.setCurrentAction(CurrentAction.DOWNLOAD);
                int filenamesize = accumulator.readInt();
                System.out.println("filenamesize = " + filenamesize);
                byte[] buf = new byte[filenamesize];
                accumulator.readBytes(buf);
                clientStatus.setCurrentFileName(new String(buf, StandardCharsets.UTF_8));
                System.out.println(clientStatus.getCurrentFileName());
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
            }
            if(signalByte == Command.storage_INFO){
                clientStatus.setCurrentAction(CurrentAction.STORAGE_INFO);

                //надо передавать и список файл и их размеры. в каком виде это лучше сделать?
                //предполагается, что при авторизации запрашивается корректный список хранимого,
                // чтобы не делать при download лишних проверок(файл точно есть и его размер известен клиенту)
            }
            return;
        }
        if(clientStatus.getCurrentAction() == CurrentAction.UPLOAD){
            RandomAccessFile raf = new RandomAccessFile(clientServer + "/" + clientStatus.getLogin() + "/" + clientStatus.getCurrentFileName(), "rw");
            raf.seek(raf.length());
            byte[] bytes = new byte[accumulator.readableBytes()];
            accumulator.readBytes(bytes);
            accumulator.clear();
            raf.write(bytes);
            if(raf.length() == clientStatus.getCurrentFileSize()){
                System.out.println("file upload done " + clientStatus.getCurrentFileName());
                clearStatusSetWaitCommand(ctx);
                sendCommand(ctx, Command.commandOK);
            }
            raf.close();
            return;
        }
//        if(clientStatus.getCurrentAction() == CurrentAction.DOWNLOAD){
//
//        }
        if(clientStatus.getCurrentAction() == CurrentAction.STORAGE_INFO){
            //TODO info
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
