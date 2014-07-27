/**********************************************************************************

   Copyright 2014 thei71

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package open.lpd.server.impl;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import open.lpd.server.LpdServerProtocol;

public class LpdServer {

	private static final String QUOTE = "\"";
	private static final String OPTION_HOST = "--host";
	private static final String OPTION_PORT = "--port";
	private static final String OPTION_SCRIPT_COMMAND = "--script";
	private static final String OPTION_QUEUE_FOLDER = "--queuefolder";
	private static final String OPTION_SOCKET_BACKLOG_SIZE = "--socketbacklogsize";
	private static final String OPTION_CLIENT_CONNECTION_THREADS = "--clientConnectionThreads";
	private static final String DEFAULT_HOST = "0.0.0.0";
	private static final String DEFAULT_PORT = "515";
	private static final String DEFAULT_QUEUE_FOLDER = "work/queues";
	private static final String DEFAULT_SCRIPT_COMMAND = "work/scripts/queue.sh $1 \"$2\"";
	private static final String DEFAULT_SOCKET_BACKLOG_SIZE = "100";
	private static final String DEFAULT_CLIENT_CONNECTION_THREADS = "8";

	private String host;
	private int port;
	private String queueFolderName;
	private String scriptCmd;

	public LpdServer(String host, int port, String queueFolderName,
			String scriptCmd) throws IOException {
		this.host = host;
		this.port = port;
		this.queueFolderName = queueFolderName;
		this.scriptCmd = scriptCmd;
	}

	public void serveConnections(int backlogSize, int clientConnectionThreads)
			throws IOException {

		// serve LPD connections

		ExecutorService executorService = Executors
				.newFixedThreadPool(clientConnectionThreads);
		InetAddress hostAddress = Inet4Address.getByName(host);
		ServerSocket serverSocket = new ServerSocket(port, backlogSize,
				hostAddress);
		try {
			while (true) {
				final Socket clientSocket = serverSocket.accept();
				executorService.submit(new Runnable() {

					@Override
					public void run() {
						try {
							try {
								new LpdServerProtocol(clientSocket
										.getInputStream(), clientSocket
										.getOutputStream(),
										new FileBasedLpdQueue(queueFolderName,
												scriptCmd)).handle();
							} finally {
								clientSocket.close();
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
			}
		} finally {
			serverSocket.close();
		}
	}

	private static String getOption(String name, String[] args,
			String defaultValue) {
		String value = null;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase(name)) {
				value = args[i + 1];
				int j = i + 1;
				while (value.startsWith(QUOTE) && !value.endsWith(QUOTE)) {
					value += args[j];
					j++;
				}
			}
		}
		return (value != null) ? value : defaultValue;
	}

	public static void main(String[] args) {

		try {
			String host = getOption(OPTION_HOST, args, DEFAULT_HOST);
			String port = getOption(OPTION_PORT, args, DEFAULT_PORT);
			final String queueFolderName = getOption(OPTION_QUEUE_FOLDER, args,
					DEFAULT_QUEUE_FOLDER);
			final String scriptCmd = getOption(OPTION_SCRIPT_COMMAND, args,
					DEFAULT_SCRIPT_COMMAND);
			String backLogSize = getOption(OPTION_SOCKET_BACKLOG_SIZE, args,
					DEFAULT_SOCKET_BACKLOG_SIZE);
			String clientConnectionThreads = getOption(
					OPTION_CLIENT_CONNECTION_THREADS, args,
					DEFAULT_CLIENT_CONNECTION_THREADS);

			// run server

			LpdServer lpdServer = new LpdServer(host, Integer.valueOf(port),
					queueFolderName, scriptCmd);
			lpdServer.serveConnections(Integer.valueOf(backLogSize),
					Integer.valueOf(clientConnectionThreads));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
