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
            }
            if(signalByte == (byte) 67){
                clientStatus.setCurrentAction(CurrentAction.STORAGE_INFO);
                //TODO info
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
            //System.out.println("write" + raf.length());
            if(raf.length() == clientStatus.getCurrentFileSize()){
                System.out.println("file upload done " + clientStatus.getCurrentFileName());
                clientStatus.setCurrentAction(CurrentAction.WAIT);
                clientStatus.setCurrentFileSize(-1);
                clientStatus.setCurrentFileName(null);
                raf.close();
                ctx.pipeline().get(FirstHandler.class).setAccumulatorCapacity(ctx,ctx.pipeline().get(FirstHandler.class).COMMAND_ACC_CAPACITY, ctx.pipeline().get(FirstHandler.class).COMMAND_ACC_CAPACITY);
                ctx.pipeline().get(FirstHandler.class).getClientStatus().setCurrentAction(CurrentAction.COMMAND_SIZE);
                ctx.writeAndFlush(bytesOK);
                return;
            }

        }
        if(clientStatus.getCurrentAction() == CurrentAction.DOWNLOAD){
            //TODO download
        }
        if(clientStatus.getCurrentAction() == CurrentAction.STORAGE_INFO){
            //TODO info
        }
    }
}
