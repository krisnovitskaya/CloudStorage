import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class FileStorageService {
    private static final String clientServer = "cloudserver_logins";



    public static void sendToClientFile(ChannelHandlerContext ctx, ClientStatus clientStatus, ChannelFutureListener finishListener) throws IOException {
        Path path = Paths.get(clientServer + "/" + clientStatus.getLogin() + "/" + clientStatus.getCurrentFileName());
        FileRegion region = new DefaultFileRegion(path.toFile(), 0, Files.size(path));
        ChannelFuture future = ctx.channel().writeAndFlush(region);
        if (finishListener != null) {
            future.addListener(finishListener);
        }
    }

    public static void uploadFile(ChannelHandlerContext ctx, ClientStatus clientStatus, ByteBuf accumulator, byte[] bytes, Callback callback) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(clientServer + "/" + clientStatus.getLogin() + "/" + clientStatus.getCurrentFileName(), "rw")) {
            raf.seek(raf.length());
            int readableBytes = accumulator.readableBytes();
            accumulator.readBytes(bytes, 0, readableBytes);
            accumulator.clear();
            raf.write(bytes, 0, readableBytes);
            System.out.println("write readable " + readableBytes);
            System.out.println(raf.length());
            if (raf.length() == clientStatus.getCurrentFileSize()) {
                callback.callback();
            }
        }
    }

    public static void sendStorageInfo(ChannelHandlerContext ctx, ClientStatus clientStatus, Callback callback) throws IOException {
        String directoryList = Files.list(Paths.get(clientServer + "/" + clientStatus.getLogin()))
                .filter(p -> Files.isDirectory(p))
                .map(p -> p.getFileName() + " -1" )
                .collect(Collectors.joining(" "));

        String filesList = Files.list(Paths.get(clientServer + "/" + clientStatus.getLogin()))
                .filter(p -> !Files.isDirectory(p))
                .map(Path::toFile)
                .map(file -> file.getName() + " " + file.length() )
                .collect(Collectors.joining(" "));


        String storage_info = (directoryList + " " + filesList).trim();
        ByteBuf buf = null;
        buf = ByteBufAllocator.DEFAULT.directBuffer(4 + storage_info.getBytes(StandardCharsets.UTF_8).length);
        buf.writeInt(storage_info.getBytes(StandardCharsets.UTF_8).length);
        buf.writeBytes(storage_info.getBytes(StandardCharsets.UTF_8));
        ctx.writeAndFlush(buf);
        callback.callback();
    }

    public static void deleteFile(ClientStatus clientStatus, Callback callback) throws IOException {
        boolean boll = Files.deleteIfExists(Paths.get(clientServer + "/" + clientStatus.getLogin() + "/" + clientStatus.getCurrentFileName()));
        if(boll) callback.callback();
    }


    public static void renameFile(ClientStatus clientStatus){
        Path path = Paths.get(clientServer + "/" + clientStatus.getLogin() + "/" + clientStatus.getCurrentFileName());
    }

}
