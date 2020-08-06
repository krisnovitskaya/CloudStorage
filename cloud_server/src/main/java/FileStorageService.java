import io.netty.channel.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileStorageService {
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
