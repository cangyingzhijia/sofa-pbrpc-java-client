// Copyright (c) CangKui <cangyingzhijia@126.com>
// All rights reserved.
//
// Author: cangyingzhijia@126.com
// Date: 2015-11-11
//
package com.cangyingzhijia.rpc.sofa;

import java.util.concurrent.atomic.AtomicBoolean;

import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

/**
 * RpcController的实现,简单实现以满足sofa-rpc的解析需要
 */
public class SofaRpcController implements RpcController {

	private Message request = null;
	private RpcCallback<Message> done = null;
	private long sequenceId = 0;
	private MethodDescriptor method = null;
	private Message responseMessage = null;
	private String reason = null;
	private Long timeoutTimePoint;
	private AtomicBoolean doneIsCalled = new AtomicBoolean(false);
	private int errorCode = RpcErrorCode.RPC_SUCCESS;
	private  SessionManager.Connection connection = null;
	
	@Override
	public void reset() {
		request = null;
		done = null;
		sequenceId = -1;
		method = null;
		responseMessage = null;
		reason = null;
		timeoutTimePoint = -1l;
		doneIsCalled.set(false);
		errorCode = RpcErrorCode.RPC_SUCCESS;
		connection = null;
	}

	@Override
	public boolean failed() {
		return errorCode != RpcErrorCode.RPC_SUCCESS;
	}

	@Override
	public String errorText() {
		if (reason == null) {
			reason = RpcErrorCode.rpcErrorCodeToString(errorCode);
		}
		return reason;
	}

	@Override
	public void startCancel() {
	}

	@Override
	public void setFailed(String reason) {
		this.reason = reason;
	}

	@Override
	public boolean isCanceled() {
		return false;
	}

	@Override
	public void notifyOnCancel(RpcCallback<Object> callback) {
	}

	void setRequestMessage(Message request) {
		this.request = request;
	}

	void setResponseMessage(Message responseMessage) {
		this.responseMessage = responseMessage;
	}

	Message getResponseMessage() {
		return responseMessage;
	}

	void setRpcCallback(RpcCallback<Message> done) {
		this.done = done;
	}

	boolean rpcCallback() {
		boolean ret = doneIsCalled.compareAndSet(false, true);
		if (ret) {
			done.run(responseMessage);
		}
		return ret;
	}

	void setSequenceId(long sequenceId) {
		this.sequenceId = sequenceId;
	}

	long getSequenceId() {
		return sequenceId;
	}

	MethodDescriptor getMethod() {
		return method;
	}

	Message getRequestMessage() {
		return request;
	}

	void setMethod(MethodDescriptor method) {
		this.method = method;
	}

	long getTimeoutTimePoint() {
		return timeoutTimePoint;
	}

	void setTimeoutTimePoint(long timeoutTimePoint) {
		this.timeoutTimePoint = timeoutTimePoint;
	}
	
	int getErrorCode() {
		return errorCode;
	}

	void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	SessionManager.Connection getConnection() {
		return connection;
	}

	void setConnection(SessionManager.Connection connection) {
		this.connection = connection;
	}

}
