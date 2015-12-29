package com.test.aio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class ReadCompletionHandler implements CompletionHandler<Integer, ByteBuffer> {

	private AsynchronousSocketChannel asynChannel;

	public ReadCompletionHandler(AsynchronousSocketChannel asynChannel) {
		if (this.asynChannel == null) {
			this.asynChannel = asynChannel;
		}
	}

	private void doWrite(String str){
		if (str != null && str.trim().length() > 0) {
			byte[] bytes = str.getBytes();
			ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
			writeBuffer.put(bytes);
			writeBuffer.flip();
			asynChannel.write(writeBuffer, writeBuffer, new CompletionHandler<Integer, ByteBuffer>() {

				@Override
				public void completed(Integer result, ByteBuffer attachment) {
					if(attachment.hasRemaining()){
						asynChannel.write(attachment, attachment, this);
					}
				}

				@Override
				public void failed(Throwable exc, ByteBuffer attachment) {
					try {
						asynChannel.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
		}
	}
	
	@Override
	public void completed(Integer result, ByteBuffer attachment) {
		attachment.flip();
		byte[] body = new byte[attachment.remaining()];
		attachment.get(body);
		try {
			String req = new String(body, "UTF-8");
			System.out.println("The time server receive order :" + req);
			String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(req)
					? new java.util.Date(System.currentTimeMillis()).toString() : "BAD ORDER";
			doWrite(currentTime);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void failed(Throwable exc, ByteBuffer attachment) {
		try {
			this.asynChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
