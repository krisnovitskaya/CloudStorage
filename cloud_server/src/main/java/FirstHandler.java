import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

public class FirstHandler extends ChannelInboundHandlerAdapter {
    private ClientStatus clientStatus;
    private ByteBuf accumulator;
    public final int COMMAND_ACC_CAPACITY = 4;
    private final String clientServer = "./cloudserver_logins";
    private Callback callback;

    private byte[] bytes = new byte[5*1024*1024];


    public FirstHandler(){
        clientStatus = new ClientStatus();
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
            FileStorageService.uploadFile(ctx, clientStatus, accumulator, bytes,  callback = () -> {
                System.out.println("file upload done " + clientStatus.getCurrentFileName());
                clearStatusSetWaitCommand(ctx);
                sendCommand(ctx, Command.commandOK);
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
        }
    }


    public void setAccumulatorCapacity(ChannelHandlerContext ctx, int accumulatorCapacity, int maxCapacity){
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

    void clearStatusSetWaitCommand(ChannelHandlerContext ctx){
        clientStatus.setCurrentFileSize(-1);
        clientStatus.setCurrentFileName(null);
        clientStatus.setCurrentAction(CurrentAction.COMMAND_SIZE);
        setAccumulatorCapacity(ctx, COMMAND_ACC_CAPACITY, COMMAND_ACC_CAPACITY);
    }
    void sendCommand(ChannelHandlerContext ctx, byte command) {
        ByteBuf buf = null;
        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(command);
        ctx.writeAndFlush(buf);
    }
}
