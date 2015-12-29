package com.test.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class TimeClientHandle implements Runnable {
	private String host;
	private int port;
	private Selector selector;
	private SocketChannel channel;
	private volatile boolean stop;

	public TimeClientHandle(String host, int port) {
		this.host = host == null ? "127.0.0.1" : host;
		this.port = port;
		try {
			selector = Selector.open();
			channel = SocketChannel.open();
			channel.configureBlocking(false);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void doConnect() throws IOException {
		if (channel.connect(new InetSocketAddress(host, port))) {
			channel.register(selector, SelectionKey.OP_READ);
			doWrite(channel);
		} else {
			channel.register(selector, SelectionKey.OP_CONNECT);
		}
	}

	private void doWrite(SocketChannel channel) throws IOException {
		byte[] req = "QUERY TIME ORDER".getBytes();
		ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
		writeBuffer.put(req);
		writeBuffer.flip();
		channel.write(writeBuffer);
		if (!writeBuffer.hasRemaining()) {
			System.out.println("Send order to server succeed.");
		}
	}

	private void handleInput(SelectionKey key) throws ClosedChannelException, IOException {
		if (key.isValid()) {
			SocketChannel channel = (SocketChannel) key.channel();
			if (key.isConnectable()) {
				if (channel.finishConnect()) {
					channel.register(selector, SelectionKey.OP_READ);
					doWrite(channel);
				} else {
					System.exit(1);
				}
			}
			if (key.isReadable()) {
				ByteBuffer readBuffer = ByteBuffer.allocate(1024);
				int readBytes = channel.read(readBuffer);
				if (readBytes > 0) {
					readBuffer.flip();
					byte[] bytes = new byte[readBuffer.remaining()];
					readBuffer.get(bytes);
					String body = new String(bytes, "UTF-8");
					System.out.println("Now is " + body);
					this.stop = true;
				} else if (readBytes < 0) {
					key.cancel();
					channel.close();
				} else {
					;
				}
			}
		}
	}

	@Override
	public void run() {
		try {
			doConnect();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		while (!stop) {
			try {
				selector.select(1000);
				Set<SelectionKey> selectKeys = selector.selectedKeys();
				Iterator<SelectionKey> it = selectKeys.iterator();
				SelectionKey key = null;
				while (it.hasNext()) {
					key = it.next();
					it.remove();
					try {
						handleInput(key);
					} catch (Exception e) {
						if (key != null) {
							key.cancel();
							if (key.channel() != null) {
								key.channel().close();
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		if(selector != null){
			try {
				selector.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
