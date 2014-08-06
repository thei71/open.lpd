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
package open.lpd.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Use the LpdClientProtocol class to implement your own LPD client.
 */
public class LpdClientProtocol {

	public static final byte CMD_PRINT_ANY_WAITING_JOBS = 1;
	public static final byte CMD_RECEIVE_A_PRINTER_JOB = 2;
	public static final byte CMD_SEND_QUEUE_STATE_SHORT = 3;
	public static final byte CMD_SEND_QUEUE_STATE_LONG = 4;
	public static final byte CMD_REMOVE_JOBS = 5;
	public static final byte SUB_CMD_ABORT_JOB = 1;
	public static final byte SUB_CMD_RECEIVE_CONTROL_FILE = 2;
	public static final byte SUB_CMD_RECEIVE_DATA_FILE = 3;
	public static final String LPD_DEFAULT_CHARSET = "ASCII";
	public static final String DEFAULT_CLIENT_HOST = "LOCALHOST";
	public static final byte ACK_SUCCESS = 0;
	public static final char LPD_LF = 0x0a;
	public static final char LPD_WHITESPACE = ' ';

	private InputStream serverInStream;
	private OutputStream serverOutStream;
	private String protocolCharset;
	private boolean sendDataFirst;
	private String clientHost;
	private String user;

	/**
	 * Creates a LPD client protocol that serves a single server connection.
	 * 
	 * @param serverOutStream
	 *            the server out stream.
	 * @param serverInStream
	 *            the server in stream.
	 */
	public LpdClientProtocol(InputStream serverInStream,
			OutputStream serverOutStream) {
		this.serverInStream = serverInStream;
		this.serverOutStream = serverOutStream;
		protocolCharset = LPD_DEFAULT_CHARSET;
		clientHost = DEFAULT_CLIENT_HOST;
		sendDataFirst = false;
		user = null;
	}

	/**
	 * Sets the charset to use for the protocol.
	 * 
	 * @param protocolCharset
	 *            the charset to use.
	 */
	public void setCharset(String protocolCharset) {
		this.protocolCharset = protocolCharset;
	}

	/**
	 * Tells the protocol to send the data file first.
	 * 
	 * @param sendDataFirst
	 *            send data file first.
	 */
	public void setSendDataFirst(boolean sendDataFirst) {
		this.sendDataFirst = sendDataFirst;
	}

	/**
	 * Sets the client host name.
	 * 
	 * @param clientHost
	 *            the client host name.
	 */
	public void setClientHost(String clientHost) {
		this.clientHost = clientHost;
	}

	/**
	 * Sets the user name.
	 * 
	 * @param user
	 *            the user name.
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * Sends the print queue command.
	 * 
	 * @param queue
	 *            the queue name.
	 * @throws IOException
	 *             throws if an I/O error happens during the protocol.
	 */
	public void printQueue(String queue) throws IOException {

		// +----+-------+----+
		// | 01 | Queue | LF |
		// +----+-------+----+
		// Command code - 1
		// Operand - Printer queue name
		//
		// This command starts the printing process if it not already running.

		serverOutStream.write(CMD_PRINT_ANY_WAITING_JOBS);
		serverOutStream.write(queue.getBytes(protocolCharset));
		serverOutStream.write(LPD_LF);
		serverOutStream.flush();
	}

	/**
	 * Sends the send file command.
	 * 
	 * @param queue
	 *            the queue name.
	 * @param name
	 *            the name of the file.
	 * @param dataStream
	 *            the data stream that contains the file data.
	 * @param dataStreamSize
	 *            the length of the data stream.
	 * @throws IOException
	 *             throws if an I/O error happens during the protocol.
	 */
	public void sendFile(String queue, String name, InputStream dataStream,
			long dataStreamSize) throws IOException {

		// create control file

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bos.write(("H" + clientHost + LPD_LF).getBytes(protocolCharset));
		bos.write(("J" + name + LPD_LF).getBytes(protocolCharset));
		bos.write(("N" + name + LPD_LF).getBytes(protocolCharset));
		if (user != null) {
			bos.write(("P" + user + LPD_LF).getBytes(protocolCharset));
		}
		byte[] controlFile = bos.toByteArray();
		ByteArrayInputStream controlStream = new ByteArrayInputStream(
				controlFile);
		sendFile(queue, dataStream, dataStreamSize, controlStream,
				controlFile.length);
	}

	/**
	 * Sends the send file command.
	 * 
	 * @param queue
	 *            the queue name.
	 * @param dataStream
	 *            the data stream that contains the file data.
	 * @param dataStreamSize
	 *            the length of the data stream.
	 * @param controlStream
	 *            the control stream that contains the control data.
	 * @param controlStreamSize
	 *            the length of the control stream.
	 * @throws IOException
	 *             throws if an I/O error happens during the protocol.
	 */
	public void sendFile(String queue, InputStream dataStream,
			long dataStreamSize, InputStream controlStream,
			long controlStreamSize) throws IOException {

		// +----+-------+----+
		// | 02 | Queue | LF |
		// +----+-------+----+
		// Command code - 2
		// Operand - Printer queue name
		//
		// Receiving a job is controlled by a second level of commands. The
		// daemon is given commands by sending them over the same connection.
		// The commands are described in the next section (6).
		//
		// After this command is sent, the client must read an acknowledgement
		// octet from the daemon. A positive acknowledgement is an octet of
		// zero bits. A negative acknowledgement is an octet of any other
		// pattern.

		serverOutStream.write(CMD_RECEIVE_A_PRINTER_JOB);
		serverOutStream.write(queue.getBytes(protocolCharset));
		serverOutStream.write(LPD_LF);
		serverOutStream.flush();
		byte[] ack = readResponse(1);
		if (ack[0] != ACK_SUCCESS) {
			throw new IOException("Received invalid ack: " + ack[0]);
		}
		if (sendDataFirst) {
			sendDataFile(dataStream, dataStreamSize);
			sendControlFile(controlStream, controlStreamSize);
		} else {
			sendControlFile(controlStream, controlStreamSize);
			sendDataFile(dataStream, dataStreamSize);
		}
	}

	private void sendControlFile(InputStream controlStream,
			long controlStreamSize) throws IOException,
			UnsupportedEncodingException {

		// +----+-------+----+------+----+
		// | 02 | Count | SP | Name | LF |
		// +----+-------+----+------+----+
		// Command code - 2
		// Operand 1 - Number of bytes in control file
		// Operand 2 - Name of control file
		//
		// The control file must be an ASCII stream with the ends of lines
		// indicated by ASCII LF. The total number of bytes in the stream is
		// sent as the first operand. The name of the control file is sent as
		// the second. It should start with ASCII "cfA", followed by a three
		// digit job number, followed by the host name which has constructed the
		// control file. Acknowledgement processing must occur as usual after
		// the command is sent.
		//
		// The next "Operand 1" octets over the same TCP connection are the
		// intended contents of the control file. Once all of the contents have
		// been delivered, an octet of zero bits is sent as an indication that
		// the file being sent is complete. A second level of acknowledgement
		// processing must occur at this point.

		String controlFileName = "cfA000" + clientHost;
		serverOutStream.write(SUB_CMD_RECEIVE_CONTROL_FILE);
		serverOutStream.write(Long.toString(controlStreamSize).getBytes(
				protocolCharset));
		serverOutStream.write(LPD_WHITESPACE);
		serverOutStream.write(controlFileName.getBytes(protocolCharset));
		serverOutStream.write(LPD_LF);
		serverOutStream.flush();
		byte[] ack = readResponse(1);
		if (ack[0] != ACK_SUCCESS) {
			throw new IOException("Received invalid ack: " + ack[0]);
		}
		long bread = 0;
		while (bread < controlStreamSize) {
			int b = controlStream.read();
			if (b == -1) {
				break;
			}
			serverOutStream.write(b);
			bread++;
		}
		serverOutStream.write(ACK_SUCCESS);
		serverOutStream.flush();
		ack = readResponse(1);
		if (ack[0] != ACK_SUCCESS) {
			throw new IOException("Received invalid ack: " + ack[0]);
		}
		if (bread != controlStreamSize) {
			abortPrintJob();
		}
	}

	private void sendDataFile(InputStream dataStream, long dataStreamSize)
			throws IOException, UnsupportedEncodingException {
		byte[] ack;

		// +----+-------+----+------+----+
		// | 03 | Count | SP | Name | LF |
		// +----+-------+----+------+----+
		// Command code - 3
		// Operand 1 - Number of bytes in data file
		// Operand 2 - Name of data file
		//
		// The data file may contain any 8 bit values at all. The total number
		// of bytes in the stream may be sent as the first operand, otherwise
		// the field should be cleared to 0. The name of the data file should
		// start with ASCII "dfA". This should be followed by a three digit job
		// number. The job number should be followed by the host name which has
		// constructed the data file. Interpretation of the contents of the
		// data file is determined by the contents of the corresponding control
		// file. If a data file length has been specified, the next "Operand 1"
		// octets over the same TCP connection are the intended contents of the
		// data file. In this case, once all of the contents have been
		// delivered, an octet of zero bits is sent as an indication that the
		// file being sent is complete. A second level of acknowledgement
		// processing must occur at this point.

		String dataFileName = "dfA000" + clientHost;
		serverOutStream.write(SUB_CMD_RECEIVE_DATA_FILE);
		serverOutStream.write(Long.toString(dataStreamSize).getBytes(
				protocolCharset));
		serverOutStream.write(LPD_WHITESPACE);
		serverOutStream.write(dataFileName.getBytes(protocolCharset));
		serverOutStream.write(LPD_LF);
		serverOutStream.flush();
		ack = readResponse(1);
		if (ack[0] != ACK_SUCCESS) {
			throw new IOException("Received invalid ack: " + ack[0]);
		}
		long bread = 0;
		while (bread < dataStreamSize) {
			int b = dataStream.read();
			if (b == -1) {
				break;
			}
			serverOutStream.write(b);
			bread++;
		}
		serverOutStream.write(ACK_SUCCESS);
		serverOutStream.flush();
		ack = readResponse(1);
		if (ack[0] != ACK_SUCCESS) {
			throw new IOException("Received invalid ack: " + ack[0]);
		}
		if (bread != dataStreamSize) {
			abortPrintJob();
		}
	}

	private void abortPrintJob() throws IOException {

		// +----+----+
		// | 01 | LF |
		// +----+----+
		// Command code - 1
		//
		// No operands should be supplied. This subcommand will remove any
		// files which have been created during this "Receive job" command.

		serverOutStream.write(SUB_CMD_ABORT_JOB);
		serverOutStream.write(LPD_LF);
		serverOutStream.flush();
	}

	/**
	 * Sends the get short queue state command.
	 * 
	 * @param queue
	 *            the queue name.
	 * @param jobs
	 *            the job list.
	 * @return the short queue state.
	 * @throws IOException
	 *             throws if an I/O error happens during the protocol.
	 */
	public String getShortQueueState(String queue, String jobs)
			throws IOException {

		// +----+-------+----+------+----+
		// | 03 | Queue | SP | List | LF |
		// +----+-------+----+------+----+
		// Command code - 3
		// Operand 1 - Printer queue name
		// Other operands - User names or job numbers
		//
		// If the user names or job numbers or both are supplied then only those
		// jobs for those users or with those numbers will be sent.
		//
		// The response is an ASCII stream which describes the printer queue.
		// The stream continues until the connection closes. Ends of lines are
		// indicated with ASCII LF control characters. The lines may also
		// contain ASCII HT control characters.

		serverOutStream.write(CMD_SEND_QUEUE_STATE_SHORT);
		serverOutStream.write(queue.getBytes(protocolCharset));
		if (jobs != null) {
			serverOutStream.write(LPD_WHITESPACE);
			serverOutStream.write(jobs.getBytes(protocolCharset));
		}
		serverOutStream.write(LPD_LF);
		serverOutStream.flush();
		byte[] response = readResponse(null);
		return new String(response, protocolCharset);
	}

	/**
	 * Sends the get long queue state command.
	 * 
	 * @param queue
	 *            the queue name.
	 * @param jobs
	 *            the job list.
	 * @return the long queue state.
	 * @throws IOException
	 *             throws if an I/O error happens during the protocol.
	 */
	public String getLongQueueState(String queue, String jobs)
			throws IOException {

		// +----+-------+----+------+----+
		// | 04 | Queue | SP | List | LF |
		// +----+-------+----+------+----+
		// Command code - 4
		// Operand 1 - Printer queue name
		// Other operands - User names or job numbers
		//
		// If the user names or job numbers or both are supplied then only those
		// jobs for those users or with those numbers will be sent.
		//
		// The response is an ASCII stream which describes the printer queue.
		// The stream continues until the connection closes. Ends of lines are
		// indicated with ASCII LF control characters. The lines may also
		// contain ASCII HT control characters.

		serverOutStream.write(CMD_SEND_QUEUE_STATE_LONG);
		serverOutStream.write(queue.getBytes(protocolCharset));
		if (jobs != null) {
			serverOutStream.write(LPD_WHITESPACE);
			serverOutStream.write(jobs.getBytes(protocolCharset));
		}
		serverOutStream.write(LPD_LF);
		serverOutStream.flush();
		byte[] response = readResponse(null);
		return new String(response, protocolCharset);
	}

	/**
	 * Sends the remove jobs command.
	 * 
	 * @param queue
	 *            the queue name.
	 * @param jobs
	 *            the job list.
	 * @throws IOException
	 *             throws if an I/O error happens during the protocol.
	 */
	public void removeJobs(String queue, String jobs) throws IOException {

		// +----+-------+----+-------+----+------+----+
		// | 05 | Queue | SP | Agent | SP | List | LF |
		// +----+-------+----+-------+----+------+----+
		// Command code - 5
		// Operand 1 - Printer queue name
		// Operand 2 - User name making request (the agent)
		// Other operands - User names or job numbers
		//
		// This command deletes the print jobs from the specified queue which
		// are listed as the other operands. If only the agent is given, the
		// command is to delete the currently active job. Unless the agent is
		// "root", it is not possible to delete a job which is not owned by the
		// user. This is also the case for specifying user names instead of
		// numbers. That is, agent "root" can delete jobs by user name but no
		// other agents can.

		serverOutStream.write(CMD_REMOVE_JOBS);
		serverOutStream.write(queue.getBytes(protocolCharset));
		serverOutStream.write(LPD_WHITESPACE);
		serverOutStream.write(user.getBytes(protocolCharset));
		if (jobs != null) {
			serverOutStream.write(LPD_WHITESPACE);
			serverOutStream.write(jobs.getBytes(protocolCharset));
		}
		serverOutStream.write(LPD_LF);
		serverOutStream.flush();
	}

	private byte[] readResponse(Integer count) throws IOException {

		// read a certain amount of bytes from the response

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		if (count == null) {
			while (true) {
				int b = serverInStream.read();
				if (b == -1) {
					break;
				}
				bos.write(b);
			}
		} else {
			for (int i = 0; i < count; i++) {
				int b = serverInStream.read();
				if (b == -1) {
					throw new IOException("Could only read " + bos.size()
							+ " out of " + count + " byte.");
				}
				bos.write(b);
			}
		}
		return bos.toByteArray();
	}
}
