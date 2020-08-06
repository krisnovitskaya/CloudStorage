import DataBase.DBService;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class AuthHandler extends ChannelInboundHandlerAdapter {
    private final byte[] bytesNo = "*".getBytes();
    private final byte[] bytesOK = "%".getBytes();
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client connected");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("try auth");
        ByteBuf accumulator = (ByteBuf)msg;
        byte[] data = new byte[accumulator.capacity()];
        accumulator.readBytes(data);
        accumulator.clear();
        String a = new String(data, StandardCharsets.UTF_8);
        System.out.println(a);
        String[] authData = new String(data, StandardCharsets.UTF_8).split(" ");   // login pass #  ili login pass &

        if (authData[2].equals("#")) {
            if (DBService.checkLoginPass(authData[0], authData[1])) {
                finishAuth(ctx, authData[0]);
                System.out.println("auth ok");
            }else{
                System.out.println("wrong log pass");
                ctx.pipeline().get(FirstHandler.class).getClientStatus().setCurrentAction(CurrentAction.COMMAND_SIZE);
                ctx.pipeline().get(FirstHandler.class).setAccumulatorCapacity(ctx,ctx.pipeline().get(FirstHandler.class).COMMAND_ACC_CAPACITY, ctx.pipeline().get(FirstHandler.class).COMMAND_ACC_CAPACITY);
                ctx.writeAndFlush(bytesNo);
            }
        }
        if (authData[2].equals("&")) {
            if (DBService.addUser(authData[0], authData[1])) {
                finishAuth(ctx, authData[0]);
                //добавить проверку и обработку уникальности логина

            }
        }
    }

    private void finishAuth(ChannelHandlerContext ctx, String login){
        ctx.pipeline().get(FirstHandler.class).getClientStatus().setCurrentAction(CurrentAction.COMMAND_SIZE);
        ctx.pipeline().get(FirstHandler.class).getClientStatus().setLogin(login);
        ctx.pipeline().addLast(new MainHandler(ctx.pipeline().get(FirstHandler.class).getClientStatus()));
        ctx.pipeline().get(FirstHandler.class).setAccumulatorCapacity(ctx,ctx.pipeline().get(FirstHandler.class).COMMAND_ACC_CAPACITY, ctx.pipeline().get(FirstHandler.class).COMMAND_ACC_CAPACITY);
        ctx.writeAndFlush(bytesOK);
        ctx.pipeline().remove(this);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
