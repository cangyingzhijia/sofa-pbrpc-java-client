// Copyright (c) CangKui <cangyingzhijia@126.com>
// All rights reserved.
//
// Author: cangyingzhijia@126.com
// Date: 2015-11-11
//
package com.cangyingzhijia.rpc.sofa;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
// import org.apache.mina.transport.socket.apr.AprSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sofa.pbrpc.SofaRpcMeta.RpcMeta;
import sofa.pbrpc.SofaBuiltinService.BuiltinService;
import sofa.pbrpc.SofaBuiltinService.HealthRequest;
import sofa.pbrpc.SofaBuiltinService.HealthResponse;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;

/**
 * SessionManager管理所有的通信会话,建立连接、发送rpc请求、接收response并回调callback，
 * 连接断开重连，与server端保持心跳，超时处理等。
 *
 */
public class SessionManager {
	
	private static int DEFAULT_CONNECT_TIMEOUT = 300;  // 连接超时，单位是毫秒300ms
	public static final long HEARTBEAT_CHECK_INTERVAL = 2000;  // 心跳检查周期，单位是毫秒 1000ms

	private SofaRpcChannel rpcChannel = null;
	private ConcurrentMap<Long, SofaRpcController> controllerMap = new ConcurrentHashMap<Long, SofaRpcController>();
	
	private ConcurrentLinkedQueue<Connection> connectedList = new ConcurrentLinkedQueue<Connection>();
	private ConcurrentLinkedQueue<Connection> disconnectedList = new ConcurrentLinkedQueue<Connection>();
	
	private AtomicLong sequenceId = new AtomicLong(0);  // 消息序号
	private IoConnector ioConnector = new NioSocketConnector();
	// private IoConnector ioConnector = new AprSocketConnector();
	
	private int serviceTimeout = 30; // 服务超时时间，单位是毫秒，默认为30ms
	private TimeoutManager timeoutManager = new TimeoutManager();
	private HeartbeatThread heartbeatThread = new HeartbeatThread();
	
	private boolean isDisposed = false;

	private final static Logger logger = LoggerFactory.getLogger(SessionManager.class);
	
	SessionManager(SofaRpcChannel rpcChannel) {
		this.rpcChannel = rpcChannel;
		ioConnector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new SofaRpcPacketProtocalCodecFactory()));
		// ioConnector.getFilterChain().addLast("logger", new LoggingFilter());
		ioConnector.setHandler(new RpcIoHandler());
		ioConnector.setConnectTimeoutMillis(DEFAULT_CONNECT_TIMEOUT);
		// timeout manager
		timeoutManager.setDaemon(true);
		
		timeoutManager.setName("timeoutManager");
		timeoutManager.start(); // start timeout manager thread
		// heartbeat thread
		heartbeatThread.setDaemon(true);
		heartbeatThread.setName("heartbeatThread");
		heartbeatThread.start();
	}
	
	void openSession(SocketAddress remoteAddress) {
		Connection con = new Connection(remoteAddress, ioConnector);
		if (con.open()) {
			connectedList.add(con);
		} else {
			disconnectedList.add(con);
		}
	}
	
	void openSession(List<SocketAddress> remoteAddressList) {
		for (SocketAddress addr : remoteAddressList) {
			openSession(addr);
		}
	}
	
	void dispose() {
		isDisposed = true;
		ioConnector.dispose(true);
		for (Connection con : connectedList) {
			con.close(true);
		}
	}
	
	private SofaRpcPacketProtocalCodecFactory.SofaRpcMessage serializeMessage(SofaRpcController rpcController) {
		RpcMeta.Builder rpcMetaBuilder = RpcMeta.newBuilder();
		rpcMetaBuilder.setType(RpcMeta.Type.REQUEST);
		rpcMetaBuilder.setSequenceId(rpcController.getSequenceId());
		String method = rpcController.getMethod().getFullName();
		rpcMetaBuilder.setMethod(method);
		byte[] metaBytes = rpcMetaBuilder.build().toByteArray();
		byte[] dataBytes = rpcController.getRequestMessage().toByteArray();
		return new SofaRpcPacketProtocalCodecFactory.SofaRpcMessage(metaBytes, dataBytes);
	}

	void callRpcMethod(SofaRpcController rpcController) {
		Long id = sequenceId.incrementAndGet();
		rpcController.setSequenceId(id);
		controllerMap.put(id, rpcController);
		timeoutManager.addController(rpcController);
		Connection con = getConnection();
		if (con != null) {
			con.write(rpcController);
		} else {
			rpcController.setErrorCode(RpcErrorCode.RPC_ERROR_SERVER_UNREACHABLE);
			rpcController.rpcCallback();
		}
	}

	private Connection getConnection() {
		Connection con = null;
		while (!connectedList.isEmpty()) {
			con = connectedList.poll();
			if (con == null) {
				continue;
			}
			if (con.isClosing()) {
				disconnectedList.add(con);
				con = null;
			} else {
				connectedList.add(con);
				break;
			}
		}
		return con;
	}
	
	/**
	 * mina IoHandler，解析收到的message，并调用rpc callback
	 *
	 */
	class RpcIoHandler extends IoHandlerAdapter {

		@Override
		public void messageReceived(IoSession session, Object message)
				throws Exception {
			SofaRpcPacketProtocalCodecFactory.SofaRpcMessage rpcMessage = (SofaRpcPacketProtocalCodecFactory.SofaRpcMessage) message;
			byte[] metaBytes = rpcMessage.getMetaBytes();
			byte[] dataBytes = rpcMessage.getDataBytes();
			RpcMeta rpcMeta = RpcMeta.parseFrom(metaBytes);
			Long sequenceId = rpcMeta.getSequenceId();
			SofaRpcController rpcController = controllerMap.remove(sequenceId);
			if (rpcController == null) {
				logger.debug(String.format("recv message not exists in controllerMap, sequenceId:%d", sequenceId));
				return;
			}
			parseReponse(rpcMeta, dataBytes, rpcController);
			rpcController.rpcCallback();  // callback
		}

		@Override
		public void exceptionCaught(IoSession session, Throwable cause)
				throws Exception {
			try {
				session.close(false);
			} catch (Exception e) {
				logger.error("RpcIoHandler exceptionCaught:", e);
			}
		}

		private void parseReponse(RpcMeta rpcMeta,
				byte[] dataBytes,
				SofaRpcController rpcController) throws InvalidProtocolBufferException {
			if (rpcMeta.getFailed()) {
				rpcController.setFailed(rpcMeta.getReason());
				rpcController.setErrorCode(rpcMeta.getErrorCode());
			}
			Message.Builder builder = rpcController.getResponseMessage().newBuilderForType();
			builder.mergeFrom(dataBytes);
			rpcController.setResponseMessage(builder.build());
		}
	}
	
	/**
	 * 超时管理器，启动一个线程check超时情况，如果超时则从controllerMap中移除请求，
	 * 并调用callback方法，此时设置controller状态为超时
	 *
	 */
	class TimeoutManager extends Thread {
		
		private ConcurrentLinkedQueue<SofaRpcController> timeoutControllerQueue = new ConcurrentLinkedQueue<SofaRpcController>();

		@Override
		public void run() {
			while (!isDisposed) {
				try {
					Long now = System.currentTimeMillis();
					SofaRpcController controller = null;
					while (!timeoutControllerQueue.isEmpty()) {
						controller = timeoutControllerQueue.peek();
						if (controller.getTimeoutTimePoint() < now) {
							doTimeout(controller);
							timeoutControllerQueue.poll();
							controller = null;
						} else {
							break;
						}
					}
					
					// wait next timeout occur
					if (controller != null) {
						long timeout = controller.getTimeoutTimePoint() - now;
						synchronized (this) {
							this.wait((int) timeout);
						}
					} else {
						synchronized (this) {
							this.wait();
						}
					}

				} catch (Exception e) {
					logger.error("do Timeout error", e);
				}
				
			}
		}
		
		void addController(SofaRpcController controller) {
			long now = System.currentTimeMillis();
			controller.setTimeoutTimePoint(serviceTimeout + now);
			timeoutControllerQueue.add(controller);
			synchronized (this) {
				this.notify();
			}
		}
		
		private void doTimeout(SofaRpcController controller) {
			Long id = controller.getSequenceId();
			if (controllerMap.remove(id) != null) {
				controller.setErrorCode(RpcErrorCode.RPC_ERROR_REQUEST_TIMEOUT);
				controller.rpcCallback();
			}
		}
	};

	/**
	 * 心跳检查线程，周期性检查服务端是否正常，及从新打开已经关闭的session
	 *
	 */
	class HeartbeatThread extends Thread {
		@Override
		public void run() {
			while (!isDisposed) {
				try {
					Thread.sleep(HEARTBEAT_CHECK_INTERVAL);
					reopenDisconnectedSession();
					heartbeatCheck();				
				} catch (Exception e) {
					logger.error("HeartbeatThread error", e);
				}
			}
		}
		
		private void heartbeatCheck() {
			BuiltinService service = BuiltinService.newStub(rpcChannel);
			final SofaRpcController controller = new SofaRpcController();
			HealthRequest request = HealthRequest.newBuilder().build();
			service.health(
					controller,
					request,
					new RpcCallback<sofa.pbrpc.SofaBuiltinService.HealthResponse>() {

						@Override
						public void run(HealthResponse healthResponse) {
							if (controller.getErrorCode() != RpcErrorCode.RPC_ERROR_REQUEST_TIMEOUT && 
									!healthResponse.getHealth().equals("OK")) {
								logger.error(String.format("health check error:%d %s", controller.getErrorCode(), controller.errorText()));
								Connection con = controller.getConnection();
								if (con != null) {
									con.close(true);
								}
							}
						}

					});
		}

		void reopenDisconnectedSession() {
			List<Connection> tmplist = new ArrayList<Connection>();
			while (!disconnectedList.isEmpty()) {
				Connection con = disconnectedList.poll();
				if (!con.open()) {
					// reopen fails so add to disdisconnectedList
					tmplist.add(con);
				} else {
					connectedList.add(con);
				}
			}
			disconnectedList.addAll(tmplist);

		};
		
	}

	class Connection {
		
		IoSession ioSession = null;
		SocketAddress remoteAddress = null;
		IoConnector ioConnector = null;
		
		public Connection(SocketAddress remoteAddress, IoConnector ioConnector) {
			this.remoteAddress = remoteAddress;
			this.ioConnector = ioConnector;
		}
		
		public void write(SofaRpcController rpcController) {
			rpcController.setConnection(this);
			SofaRpcPacketProtocalCodecFactory.SofaRpcMessage msg = serializeMessage(rpcController);
			ioSession.write(msg);
		}

		public boolean isClosing() {
			return ioSession.isClosing();
		}

		public void close(boolean immediately) {
			ioSession.close(immediately);
		}

		boolean open() {
			boolean ret = false;
			try {
				if (ioSession != null) {
					ioSession.close(true);
					ioSession = null;
				}
				
				ConnectFuture connFuture = ioConnector.connect(remoteAddress);
				connFuture.awaitUninterruptibly();
				ioSession = connFuture.getSession();
				ret = ioSession.isConnected();
			} catch (Exception e) {
				logger.error(String.format("open session error:%s", remoteAddress), e);
			}
			return ret;
		}
		
	}
	
	void setServiceTimeout(int serviceTimeout) {
		this.serviceTimeout = serviceTimeout;
	}

}
