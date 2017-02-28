// Copyright (c) CangKui <cangyingzhijia@126.com>
// All rights reserved.
//
// Author: cangyingzhijia@126.com
// Date: 2015-11-11
//
package com.cangyingzhijia.rpc.sofa;

public class RpcErrorCode {
	public static final int RPC_SUCCESS = 0;
	public static final int RPC_ERROR_PARSE_REQUEST_MESSAGE = 1;
	public static final int RPC_ERROR_PARSE_RESPONSE_MESSAGE = 2;
	public static final int RPC_ERROR_UNCOMPRESS_MESSAGE = 3;
	public static final int RPC_ERROR_COMPRESS_TYPE = 4;
	public static final int RPC_ERROR_NOT_SPECIFY_METHOD_NAME = 5;
	public static final int RPC_ERROR_PARSE_METHOD_NAME = 6;
	public static final int RPC_ERROR_FOUND_SERVICE = 7;
	public static final int RPC_ERROR_FOUND_METHOD = 8;
	public static final int RPC_ERROR_CHANNEL_BROKEN = 9;
	public static final int RPC_ERROR_CONNECTION_CLOSED = 10;
	public static final int RPC_ERROR_REQUEST_TIMEOUT = 11; // request timeout
	public static final int RPC_ERROR_REQUEST_CANCELED = 12; // request canceled
	public static final int RPC_ERROR_SERVER_UNAVAILABLE = 13; // server un-healthy
	public static final int RPC_ERROR_SERVER_UNREACHABLE = 14; // server un-reachable
	public static final int RPC_ERROR_SERVER_SHUTDOWN = 15;
	public static final int RPC_ERROR_SEND_BUFFER_FULL = 16;
	public static final int RPC_ERROR_SERIALIZE_REQUEST = 17;
	public static final int RPC_ERROR_SERIALIZE_RESPONSE = 18;
	public static final int RPC_ERROR_RESOLVE_ADDRESS = 19;
	public static final int RPC_ERROR_CREATE_STREAM = 20;
	public static final int RPC_ERROR_NOT_IN_RUNNING = 21;
	public static final int RPC_ERROR_SERVER_BUSY = 22;

    // error code for listener
	public static final int RPC_ERROR_TOO_MANY_OPEN_FILES = 101;

	public static final int RPC_ERROR_UNKNOWN = 999;
	public static final int RPC_ERROR_FROM_USER = 1000;
	
	public static String rpcErrorCodeToString(int errorCode) {
		switch(errorCode)
	    {
		case RPC_SUCCESS:
			return "RPC_SUCCESS";
	    case RPC_ERROR_PARSE_REQUEST_MESSAGE:
	    	return "RPC_ERROR_PARSE_REQUEST_MESSAGE";
	    case RPC_ERROR_PARSE_RESPONSE_MESSAGE:
	    	return "RPC_ERROR_PARSE_RESPONSE_MESSAGE";
	    case RPC_ERROR_UNCOMPRESS_MESSAGE:
	    	return "RPC_ERROR_UNCOMPRESS_MESSAGE";
	    case RPC_ERROR_COMPRESS_TYPE:
	    	return "RPC_ERROR_COMPRESS_TYPE";
	    case RPC_ERROR_NOT_SPECIFY_METHOD_NAME:
	    	return "RPC_ERROR_NOT_SPECIFY_METHOD_NAME";
	    case RPC_ERROR_PARSE_METHOD_NAME:
	    	return "RPC_ERROR_PARSE_METHOD_NAME";
	    case RPC_ERROR_FOUND_SERVICE:
	    	return "RPC_ERROR_FOUND_SERVICE";
	    case RPC_ERROR_FOUND_METHOD:
	    	return "RPC_ERROR_FOUND_METHOD";
	    case RPC_ERROR_CHANNEL_BROKEN:
	    	return "RPC_ERROR_CHANNEL_BROKEN";
	    case RPC_ERROR_CONNECTION_CLOSED:
	    	return "RPC_ERROR_CONNECTION_CLOSED";
	    case RPC_ERROR_REQUEST_TIMEOUT:
	    	return "RPC_ERROR_REQUEST_TIMEOUT";
	    case RPC_ERROR_REQUEST_CANCELED:
	    	return "RPC_ERROR_REQUEST_CANCELED";
	    case RPC_ERROR_SERVER_UNAVAILABLE:
	    	return "RPC_ERROR_SERVER_UNAVAILABLE";
	    case RPC_ERROR_SERVER_UNREACHABLE:
	    	return "RPC_ERROR_SERVER_UNREACHABLE";
	    case RPC_ERROR_SERVER_SHUTDOWN:
	    	return "RPC_ERROR_SERVER_SHUTDOWN";
	    case RPC_ERROR_SEND_BUFFER_FULL:
	    	return "RPC_ERROR_SEND_BUFFER_FULL";
	    case RPC_ERROR_SERIALIZE_REQUEST:
	    	return "RPC_ERROR_SERIALIZE_REQUEST";
	    case RPC_ERROR_SERIALIZE_RESPONSE:
	    	return "RPC_ERROR_SERIALIZE_RESPONSE";
	    case RPC_ERROR_RESOLVE_ADDRESS:
	    	return "RPC_ERROR_RESOLVE_ADDRESS";
	    case RPC_ERROR_CREATE_STREAM:
	    	return "RPC_ERROR_CREATE_STREAM";
	    case RPC_ERROR_NOT_IN_RUNNING:
	    	return "RPC_ERROR_NOT_IN_RUNNING";
	    case RPC_ERROR_SERVER_BUSY:
	    	return "RPC_ERROR_SERVER_BUSY";
	    case RPC_ERROR_TOO_MANY_OPEN_FILES:
	    	return "RPC_ERROR_TOO_MANY_OPEN_FILES";
	    case RPC_ERROR_UNKNOWN:
	    	return "RPC_ERROR_UNKNOWN";
	    case RPC_ERROR_FROM_USER:
	    	return "RPC_ERROR_FROM_USER";
	    }
	    return "RPC_ERROR_UNDEFINED";
	}
}
