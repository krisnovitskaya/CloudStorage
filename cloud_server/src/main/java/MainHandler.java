import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainHandler extends ChannelInboundHandlerAdapter {
    private ClientStatus clientStatus;
    private final byte[] bytesOK = "%".getBytes();
    private final String clientServer = "./cloudserver_logins";


    public MainHandler(ClientStatus clientStatus){
        this.clientStatus = clientStatus;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf accumulator = (ByteBuf) msg;
        if(clientStatus.getCurrentAction() == CurrentAction.WAIT){
            byte signalByte = accumulator.readByte();
            if(signalByte == (byte) 65){                        //65 == upload
                clientStatus.setCurrentAction(CurrentAction.UPLOAD);
                int filenamesize = accumulator.readInt();
                System.out.println("filenamesize = " + filenamesize);
                byte[] buf = new byte[filenamesize];
                accumulator.readBytes(buf);
                clientStatus.setCurrentFileName(new String(buf, "UTF-8"));
                System.out.println(clientStatus.getCurrentFileName());
                Path path = Paths.get(clientServer + "/" + clientStatus.getLogin() + "/" + clientStatus.getCurrentFileName());
                Files.deleteIfExists(path);
  //             если запустить TestClient 2 раза подряд, происходит это.  Выходит падает при удалении существующего файла. как лечить?
 //               авг 06, 2020 1:35:14 PM io.netty.channel.DefaultChannelPipeline onUnhandledInboundException
 //               WARNING: An exceptionCaught() event was fired, and it reached at the tail of the pipeline. It usually means the last handler in the pipeline did not handle the exception.
 //               java.nio.file.FileSystemException: .\cloudserver_logins\login1\1.txt: Процесс не может получить доступ к файлу, так как этот файл занят другим процессом.
 //               	at sun.nio.fs.WindowsException.translateToIOException(WindowsException.java:86)
                //	at sun.nio.fs.WindowsException.rethrowAsIOException(WindowsException.java:97)
                //	at sun.nio.fs.WindowsException.rethrowAsIOException(WindowsException.java:102)
                //	at sun.nio.fs.WindowsFileSystemProvider.implDelete(WindowsFileSystemProvider.java:269)
                //	at sun.nio.fs.AbstractFileSystemProvider.deleteIfExists(AbstractFileSystemProvider.java:108)
                //	at java.nio.file.Files.deleteIfExists(Files.java:1165)
                //	at MainHandler.channelRead(MainHandler.java:33)
                //.. и т.д.
                long filesize = accumulator.readLong();
                clientStatus.setCurrentFileSize(filesize);
                System.out.println("filesize = " + filesize);
                accumulator.clear();
                ctx.pipeline().get(FirstHandler.class).setAccumulatorCapacity(ctx,1024, 5 * 1024 * 1024);
                ctx.writeAndFlush(bytesOK);
            }
            if(signalByte == (byte) 66){
                clientStatus.setCurrentAction(CurrentAction.DOWNLOAD);
                //TODO download
                int filenamesize = accumulator.readInt();
                System.out.println("filenamesize = " + filenamesize);
                byte[] buf = new byte[filenamesize];
                accumulator.readBytes(buf);
                clientStatus.setCurrentFileName(new String(buf, "UTF-8"));
                System.out.println(clientStatus.getCurrentFileName());
                System.out.println("start download");
                FileStorageService.sendToClientFile(ctx, clientStatus, futureListener ->{
                    if (futureListener.isSuccess()) {
                        System.out.println("file send complete");
                        //ctx.writeAndFlush(bytesOK);
                    } else {
                        futureListener.cause().printStackTrace();
                        System.out.println("download error");
                    }
                });
                clearStatusSetWaitCommand(ctx);
            }
            if(signalByte == (byte) 67){
                clientStatus.setCurrentAction(CurrentAction.STORAGE_INFO);
                //TODO info
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
                raf.close();
                clearStatusSetWaitCommand(ctx);
                ctx.writeAndFlush(bytesOK);
                return;
            }

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
}
