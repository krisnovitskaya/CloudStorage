import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import server.Const;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class FileStorageService {
    static long start;





    public static void sendToClientFile(ChannelHandlerContext ctx, ClientStatus clientStatus, ChannelFutureListener finishListener) throws IOException {

        Path path = Paths.get(Const.CLOUD_PACKAGE + "/" + clientStatus.getCurrentDir() + "/" + clientStatus.getCurrentFileName());
        FileRegion region = new DefaultFileRegion(path.toFile(), 0, Files.size(path));
        ChannelFuture future = ctx.channel().writeAndFlush(region);
        if (finishListener != null) {
            future.addListener(finishListener);
        }
    }

    public static void uploadFile(ClientStatus clientStatus, ByteBuf accumulator, byte[] bytes, Callback callback) throws IOException {
        //try (RandomAccessFile raf = new RandomAccessFile(Const.CLOUD_PACKAGE + "/" + clientStatus.getCurrentDir() + "/" + clientStatus.getCurrentFileName(), "rw")) {

            RandomAccessFile raf = clientStatus.getUploadFile();
            if(raf.length() == 0){
                start = System.currentTimeMillis();
                System.out.println("start upload");
            }
            raf.seek(raf.length());
            int readableBytes = accumulator.readableBytes();
            accumulator.readBytes(bytes, 0, readableBytes);
            accumulator.clear();
            raf.write(bytes, 0, readableBytes);
            if (raf.length() == clientStatus.getCurrentFileSize()) {
                raf.close();
                clientStatus.setUploadFile(null);
                long end = System.currentTimeMillis();
                System.out.println("end upload");
                System.out.println((end - start)/1000 + "upload time");
                callback.callback();
            }
       // }
    }

    public static void sendStorageInfo(ClientStatus clientStatus, ChannelHandlerContext ctx, String storagePath, Callback callback) throws IOException {
        clientStatus.setCurrentDir(storagePath);
        String directoryList = Files.list(Paths.get(Const.CLOUD_PACKAGE + "/" + storagePath))
                .filter(p -> Files.isDirectory(p))
                .map(p -> p.getFileName() + ":-1" )
                .collect(Collectors.joining(":"));

        String filesList = Files.list(Paths.get(Const.CLOUD_PACKAGE + "/" + storagePath))
                .filter(p -> !Files.isDirectory(p))
                .map(Path::toFile)
                .map(file -> file.getName() + ":" + file.length() )
                .collect(Collectors.joining(":"));


        StringBuilder sb_storage_info = new StringBuilder();
        sb_storage_info.append(storagePath);
        if (directoryList.length() > 0) sb_storage_info.append(":" + directoryList);
        if (filesList.length() > 0) sb_storage_info.append(":" + filesList);

        ByteBuf buf = null;
        buf = ByteBufAllocator.DEFAULT.directBuffer(4 + sb_storage_info.toString().getBytes(StandardCharsets.UTF_8).length);
        buf.writeInt(sb_storage_info.toString().getBytes(StandardCharsets.UTF_8).length);
        buf.writeBytes(sb_storage_info.toString().getBytes(StandardCharsets.UTF_8));
        ctx.writeAndFlush(buf);
        callback.callback();
    }

    public static void deleteFile(ClientStatus clientStatus, Callback callback) throws IOException {
        boolean bool = Files.deleteIfExists(Paths.get(Const.CLOUD_PACKAGE + "/" + clientStatus.getCurrentDir() + "/" + clientStatus.getCurrentFileName()));
        if(bool) callback.callback();
    }


    public static void createDirectory(ClientStatus clientStatus, String dirName, BoolCallback callback){
        try{
            Files.createDirectory(Paths.get(Const.CLOUD_PACKAGE + "/" + clientStatus.getCurrentDir() + "/" + dirName));
            callback.callback(true);
        }catch (IOException e){
            callback.callback(false);
        }

    }

}
