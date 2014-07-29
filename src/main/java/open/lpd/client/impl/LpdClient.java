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
package open.lpd.client.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import open.lpd.client.LpdClientProtocol;

/**
 * A ready to go LPD client (aka "lpr") that can send files and standard LPD
 * commands to a LPD server.
 */
public class LpdClient {

	private static final String CMD_PRINT = "print";
	private static final String CMD_SEND = "send";
	private static final String CMD_STATE = "state";
	private static final String CMD_LSTATE = "lstate";
	private static final String CMD_REMOVE = "remove";
	private static final String OPTION_CMD = "--cmd";
	private static final String OPTION_HOST = "--host";
	private static final String OPTION_PORT = "--port";
	private static final String OPTION_QUEUE = "--queue";
	private static final String OPTION_FILE = "--file";
	private static final String OPTION_AGENT = "--agent";
	private static final String OPTION_JOBS = "--jobs";
	private static final String OPTION_DATA_FIRST = "--datafirst";
	private static final String DEFAULT_HOST = "127.0.0.1";
	private static final String DEFAULT_PORT = "515";
	private static final String DEFAULT_QUEUE = "RAW";
	private static final String DEFAULT_DATA_FIRST = "false";
	private static final String QUOTE = "\"";

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

		Socket socket = new Socket();
		try {
			// get options

			String cmd = getOption(OPTION_CMD, args, null);
			String host = getOption(OPTION_HOST, args, DEFAULT_HOST);
			String port = getOption(OPTION_PORT, args, DEFAULT_PORT);
			String queue = getOption(OPTION_QUEUE, args, DEFAULT_QUEUE);
			String file = getOption(OPTION_FILE, args, null);
			String jobs = getOption(OPTION_JOBS, args, null);
			String agent = getOption(OPTION_AGENT, args, null);
			String dataFirst = getOption(OPTION_DATA_FIRST, args,
					DEFAULT_DATA_FIRST);

			// establish connection

			socket.connect(new InetSocketAddress(host, Integer.valueOf(port)));
			OutputStream serverOutStream = socket.getOutputStream();
			InputStream serverInStream = socket.getInputStream();
			String clientHost = socket.getLocalAddress().getHostName();

			// run client

			if (queue == null) {
				throw new IllegalArgumentException(OPTION_QUEUE);
			}
			LpdClientProtocol lpdClientProtocol = new LpdClientProtocol(
					serverInStream, serverOutStream);
			if (cmd.equalsIgnoreCase(CMD_PRINT)) {
				lpdClientProtocol.printQueue(queue);
				System.out.println("Printed.");
			} else if (cmd.equalsIgnoreCase(CMD_SEND)) {
				if (agent != null) {
					lpdClientProtocol.setUser(agent);
				}
				if (clientHost != null) {
					lpdClientProtocol.setClientHost(clientHost);
				}
				lpdClientProtocol.setSendDataFirst(dataFirst
						.equalsIgnoreCase("true"));
				if (file == null) {
					throw new IllegalArgumentException(OPTION_FILE);
				}
				File fileObj = new File(file);
				if (!fileObj.exists()) {
					throw new FileNotFoundException(file);
				}
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(fileObj);
					lpdClientProtocol.sendFile(queue, fileObj.getName(),
							new BufferedInputStream(fis), fileObj.length());
				} finally {
					if (fis != null) {
						fis.close();
					}
				}
			} else if (cmd.equalsIgnoreCase(CMD_STATE)) {
				String state = lpdClientProtocol
						.getShortQueueState(queue, jobs);
				System.out.println("State:");
				System.out.println(state);
			} else if (cmd.equalsIgnoreCase(CMD_LSTATE)) {
				String lstate = lpdClientProtocol
						.getLongQueueState(queue, jobs);
				System.out.println("LState:");
				System.out.println(lstate);
			} else if (cmd.equalsIgnoreCase(CMD_REMOVE)) {
				if (agent == null) {
					throw new IllegalArgumentException(OPTION_AGENT);
				}
				lpdClientProtocol.setUser(agent);
				lpdClientProtocol.removeJobs(queue, jobs);
				System.out.println("Removed.");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
