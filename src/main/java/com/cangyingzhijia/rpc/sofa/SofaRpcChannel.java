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

import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.RpcController;


/**
 *  对RpcChannel的实现，用于连接sofa-rpc服务端，与服务端进行rpc通信<br/>
 *  example:
 *  <pre>
 *      List<SocketAddress> addrList = new ArrayList<SocketAddress>();
 *      addrList.add(new InetSocketAddress("10.0.2.11", 12345));
 *      addrList.add(new InetSocketAddress("10.0.2.12", 12345));
 *      SofaRpcChannel channel = new SofaRpcChannel(addrList);
 *      Echo.EchoService echoService = Echo.EchoService.newStub(channel);
 *      .....
 *  </pre>
 */
public class SofaRpcChannel implements RpcChannel {
	
	private SessionManager connectionManager = null;
	
	public SofaRpcChannel(SocketAddress remoteAddress) {
		List<SocketAddress> addressList = new ArrayList<SocketAddress>();
		addressList.add(remoteAddress);
		Init(addressList);
	}
	
	public SofaRpcChannel(List<SocketAddress> remoteAddressList) {
		Init(remoteAddressList);
	}
	
	private void Init(List<SocketAddress> remoteAddressList) {
		connectionManager = new SessionManager(this);
		connectionManager.openSession(remoteAddressList);
	}
	
	/**
	 * 设置服务超时时间
	 * @param serviceTimeout
	 */
	public void setServiceTimeout(int serviceTimeout) {
		connectionManager.setServiceTimeout(serviceTimeout);
	}

	/**
	 * 释放channel
	 */
	public void dispose() {
		connectionManager.dispose();
	}
	
	@Override
	public void callMethod(MethodDescriptor method, RpcController controller,
			Message request, Message responsePrototype,
			RpcCallback<Message> done) {
		SofaRpcController rpcController = (SofaRpcController) controller;
		rpcController.setMethod(method);
		rpcController.setRequestMessage(request);
		rpcController.setResponseMessage(responsePrototype);
		rpcController.setRpcCallback(done);
		connectionManager.callRpcMethod(rpcController);
	}
}
