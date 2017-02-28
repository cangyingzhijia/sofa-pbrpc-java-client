// Copyright (c) CangKui <cangyingzhijia@126.com>
// All rights reserved.
//
// Author: cangyingzhijia@126.com
// Date: 2015-11-11
//
package com.cangyingzhijia.rpc.sofa;

import java.nio.ByteOrder;
import java.util.Arrays;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * 实现mina ProtocolCodecFactory接口，提供对sofa rpc数据的序列化、反序列化操作
 * @author cangyingzhijia
 *
 */
public class SofaRpcPacketProtocalCodecFactory implements ProtocolCodecFactory {

	private static final int HEADER_SIZE = 24;
	private static final byte[] MAGIC_STR = "SOFA".getBytes();
	SofaRpcPacketProtocolDecoder protocalDecoder = new SofaRpcPacketProtocolDecoder();
	SofaRpcPacketProtocolEncoder protocalEncoder = new SofaRpcPacketProtocolEncoder();

	@Override
	public ProtocolDecoder getDecoder(IoSession arg0) throws Exception {
		return protocalDecoder;
	}

	@Override
	public ProtocolEncoder getEncoder(IoSession arg0) throws Exception {
		return protocalEncoder;
	}

	static class SofaRpcPacketProtocolDecoder extends CumulativeProtocolDecoder {

		private final AttributeKey CONTEXT = new AttributeKey(getClass(),
				"context");

		public SofaRpcPacketProtocolDecoder() {
		}

		@Override
		protected boolean doDecode(IoSession session, IoBuffer in,
				ProtocolDecoderOutput out) throws Exception {
			SofaRpcPacket ctx = getContext(session);
			boolean ret = false;
			while (in.hasRemaining()) {
				ret = ctx.decodePacket(in);
				if (ret) {
					out.write(new SofaRpcMessage(ctx.metaBytes, ctx.dataBytes));
				} else {
					break;
				}
			}
			return ret;
		}

		private SofaRpcPacket getContext(IoSession session) {
			SofaRpcPacket context = (SofaRpcPacket) session
					.getAttribute(CONTEXT);
			if (context == null) {
				context = new SofaRpcPacket();
				session.setAttribute(CONTEXT, context);
			}
			return context;
		}

		final static class SofaRpcPacket {
			
			private int metaSize = 0;
			private long dataSize = 0;
			private long totalSize = 0;
			private byte[] metaBytes = null;
			private byte[] dataBytes = null;

			enum ParseState {
				STATE_PARSE_INIT,
				STATE_PARSE_HEADER,
				STATE_PARSE_META,
				STATE_PARSE_DATA,
				STATE_PARSE_FINISH
			}

			private ParseState state = ParseState.STATE_PARSE_INIT;

			public boolean decodePacket(IoBuffer in)
					throws InvalidProtocolBufferException {
				while (in.hasRemaining()) {
					switch (state) {
					case STATE_PARSE_INIT:
						init(in);
						break;
					case STATE_PARSE_HEADER:
						if (!parseHeader(in)) {
							return false;
						}
						break;
					case STATE_PARSE_META:
						if (!parseMeta(in)) {
							return false;
						}
						break;
					case STATE_PARSE_DATA:
						if (!parseData(in)) {
							return false;
						}
						if (state == ParseState.STATE_PARSE_FINISH) {
							state = ParseState.STATE_PARSE_INIT;
							return true;
						}
						break;
					}
				}
				return false;
			}

			private boolean parseData(IoBuffer in) {
				if (in.remaining() < dataSize) {
					return false;
				}
				dataBytes = new byte[(int) dataSize];
				in.get(dataBytes);
				state = ParseState.STATE_PARSE_FINISH;
				return true;
			}

			private boolean parseHeader(IoBuffer in) {
				if (in.remaining() < HEADER_SIZE) {
					return false;
				}
				byte[] magicStr = new byte[4];
				in.get(magicStr);
				if (!Arrays.equals(magicStr, MAGIC_STR)) {
					throw new RuntimeException(String.format("magic str not match:%s", new String(magicStr)));
				}
				ByteOrder orig_order = in.order();
				in.order(ByteOrder.LITTLE_ENDIAN);
				metaSize = in.getInt();
				dataSize = in.getLong();
				totalSize = in.getLong();
				in.order(orig_order);
				if (metaSize + dataSize != totalSize) { // sanity check
					throw new RuntimeException(String.format("metaSize(%d) + dataSize(%d) != totalSize(%d),", metaSize, dataSize, totalSize));
				}
				state = ParseState.STATE_PARSE_META;
				return true;
			}

			private boolean parseMeta(IoBuffer in)
					throws InvalidProtocolBufferException {
				if (in.remaining() < metaSize) {
					return false;
				}
				metaBytes = new byte[metaSize];
				in.get(metaBytes);
				state = ParseState.STATE_PARSE_DATA;
				return true;
			}

			private void init(IoBuffer in) {
				metaSize = 0;
				dataSize = 0;
				totalSize = 0;
				metaBytes = null;
				dataBytes = null;
				state = ParseState.STATE_PARSE_HEADER;
			}
		}

	}

	static class SofaRpcPacketProtocolEncoder extends ProtocolEncoderAdapter {

		@Override
		public void encode(IoSession session, Object message,
				ProtocolEncoderOutput out) throws Exception {
			SofaRpcMessage rpcMessage = (SofaRpcMessage) message;
			byte[] meataBytes = rpcMessage.getMetaBytes();
			byte[] dataBytes = rpcMessage.getDataBytes();
			int metaSize = meataBytes.length;
			int dataSize = dataBytes.length;
			int totalSize = metaSize + dataSize;
			IoBuffer buffer = IoBuffer.allocate(totalSize + HEADER_SIZE);
			// header
			buffer.put(MAGIC_STR);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			buffer.putInt(metaSize);
			buffer.putLong(dataSize);
			buffer.putLong(totalSize);
			// meta
			buffer.put(meataBytes);
			// data
			buffer.put(dataBytes);
			buffer.flip();
			out.write(buffer);
		}
	}
	
	static final class SofaRpcMessage {
		
		private byte[] metaBytes = null;
		private byte[] dataBytes = null;
		
		public SofaRpcMessage(byte[] metaBytes, byte[] dataBytes) {
			this.metaBytes = metaBytes;
			this.dataBytes = dataBytes;
		}

		public byte[] getMetaBytes() {
			return metaBytes;
		}
		public void setMetaBytes(byte[] metaBytes) {
			this.metaBytes = metaBytes;
		}
		public byte[] getDataBytes() {
			return dataBytes;
		}
		
		public void setDataBytes(byte[] dataBytes) {
			this.dataBytes = dataBytes;
		}
	}
}


