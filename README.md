# sofa-pbrpc-java-client
java实现的sofa-pbrpc client端库，用于与sofa-pbrpc server端进行rpc调用

# Example

    package com.cangyingzhijia.rpc.sofa;

    import java.net.InetSocketAddress;
    import java.net.SocketAddress;
    import java.util.concurrent.CountDownLatch;

    import com.google.protobuf.RpcCallback;

    import echo.Echo;
    import echo.Echo.EchoMessage;

    public class RpcClientTest {

    	public static void main(String[] args) throws InterruptedException {
    		SocketAddress socketAddress = new InetSocketAddress("10.0.2.11", 12345);
    		SofaRpcChannel channel = new SofaRpcChannel(socketAddress);
    		channel.setServiceTimeout(80);

    		Echo.EchoService echoService = Echo.EchoService.newStub(channel);

    		final SofaRpcController controller = new SofaRpcController();

    		EchoMessage.Builder builder = EchoMessage.newBuilder();
    		builder.setData("helloworld");
    		EchoMessage request = builder.build();

    		final CountDownLatch latch = new CountDownLatch(1);
    		echoService.echo(controller,
    				request,
    				new RpcCallback<echo.Echo.EchoMessage>() {
    					@Override
    					public void run(EchoMessage echoMessage) {
    						if (controller.failed()) {
    							System.err.printf("rpc fails:%s\n", controller.errorText());
    						} else {
							System.err.printf("recv:%s\n",echoMessage);
						}
						latch.countDown();
					}
				});

		    latch.await();
		    channel.dispose();
	    }
    }
