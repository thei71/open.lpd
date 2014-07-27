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

	private String lpdCharset;
	private boolean sendDataFirst;
	private String clientHost;
	private String user;

	public LpdClientProtocol() {
		lpdCharset = LPD_DEFAULT_CHARSET;
		sendDataFirst = false;
		clientHost = DEFAULT_CLIENT_HOST;
		user = null;
	}

	public void setCharset(String charsetName) {
		lpdCharset = charsetName;
	}

	public void setSendDataFirst(boolean sendDataFirst) {
		this.sendDataFirst = sendDataFirst;
	}

	public void setClientHost(String clientHost) {
		this.clientHost = clientHost;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public void printQueue(OutputStream os, String queue) throws IOException {

		// +----+-------+----+
		// | 01 | Queue | LF |
		// +----+-------+----+
		// Command code - 1
		// Operand - Printer queue name
		//
		// This command starts the printing process if it not already running.

		os.write(CMD_PRINT_ANY_WAITING_JOBS);
		os.write(queue.getBytes(lpdCharset));
		os.write(LPD_LF);
		os.flush();
	}

	public void sendFile(OutputStream os, InputStream is, String queue,
			String name, InputStream dataStream, long dataStreamSize)
			throws IOException {

		// create control file

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bos.write(("H" + clientHost + LPD_LF).getBytes(lpdCharset));
		bos.write(("J" + name + LPD_LF).getBytes(lpdCharset));
		bos.write(("N" + name + LPD_LF).getBytes(lpdCharset));
		if (user != null) {
			bos.write(("P" + user + LPD_LF).getBytes(lpdCharset));
		}
		byte[] controlFile = bos.toByteArray();
		ByteArrayInputStream controlStream = new ByteArrayInputStream(
				controlFile);
		sendFile(os, is, queue, dataStream, dataStreamSize, controlStream,
				controlFile.length);
	}

	public void sendFile(OutputStream os, InputStream is, String queue,
			InputStream dataStream, long dataStreamSize,
			InputStream controlStream, long controlStreamSize)
			throws IOException {

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

		os.write(CMD_RECEIVE_A_PRINTER_JOB);
		os.write(queue.getBytes(lpdCharset));
		os.write(LPD_LF);
		os.flush();
		byte[] ack = readResponse(is, 1);
		if (ack[0] != ACK_SUCCESS) {
			throw new IOException("Received invalid ack: " + ack);
		}
		if (sendDataFirst) {
			sendDataFile(os, is, dataStream, dataStreamSize);
			sendControlFile(os, is, controlStream, controlStreamSize);
		} else {
			sendControlFile(os, is, controlStream, controlStreamSize);
			sendDataFile(os, is, dataStream, dataStreamSize);
		}
	}

	private void sendControlFile(OutputStream os, InputStream is,
			InputStream controlStream, long controlStreamSize)
			throws IOException, UnsupportedEncodingException {

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
		os.write(SUB_CMD_RECEIVE_CONTROL_FILE);
		os.write(Long.toString(controlStreamSize).getBytes(lpdCharset));
		os.write(LPD_WHITESPACE);
		os.write(controlFileName.getBytes(lpdCharset));
		os.write(LPD_LF);
		os.flush();
		byte[] ack = readResponse(is, 1);
		if (ack[0] != ACK_SUCCESS) {
			throw new IOException("Received invalid ack: " + ack);
		}
		long bread = 0;
		while (bread < controlStreamSize) {
			int b = controlStream.read();
			if (b == -1) {
				break;
			}
			os.write(b);
			bread++;
		}
		os.write(ACK_SUCCESS);
		os.flush();
		ack = readResponse(is, 1);
		if (ack[0] != ACK_SUCCESS) {
			throw new IOException("Received invalid ack: " + ack);
		}
		if (bread != controlStreamSize) {
			abortPrintJob(os);
		}
	}

	private void sendDataFile(OutputStream os, InputStream is,
			InputStream dataStream, long dataStreamSize) throws IOException,
			UnsupportedEncodingException {
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
		os.write(SUB_CMD_RECEIVE_DATA_FILE);
		os.write(Long.toString(dataStreamSize).getBytes(lpdCharset));
		os.write(LPD_WHITESPACE);
		os.write(dataFileName.getBytes(lpdCharset));
		os.write(LPD_LF);
		os.flush();
		ack = readResponse(is, 1);
		if (ack[0] != ACK_SUCCESS) {
			throw new IOException("Received invalid ack: " + ack);
		}
		long bread = 0;
		while (bread < dataStreamSize) {
			int b = dataStream.read();
			if (b == -1) {
				break;
			}
			os.write(b);
			bread++;
		}
		os.write(ACK_SUCCESS);
		os.flush();
		ack = readResponse(is, 1);
		if (ack[0] != ACK_SUCCESS) {
			throw new IOException("Received invalid ack: " + ack);
		}
		if (bread != dataStreamSize) {
			abortPrintJob(os);
		}
	}

	private void abortPrintJob(OutputStream os) throws IOException {

		// +----+----+
		// | 01 | LF |
		// +----+----+
		// Command code - 1
		//
		// No operands should be supplied. This subcommand will remove any
		// files which have been created during this "Receive job" command.

		os.write(SUB_CMD_ABORT_JOB);
		os.write(LPD_LF);
		os.flush();
	}

	public String getShortQueueState(OutputStream os, InputStream is,
			String queue, String jobs) throws IOException {

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

		os.write(CMD_SEND_QUEUE_STATE_SHORT);
		os.write(queue.getBytes(lpdCharset));
		if (jobs != null) {
			os.write(LPD_WHITESPACE);
			os.write(jobs.getBytes(lpdCharset));
		}
		os.write(LPD_LF);
		os.flush();
		byte[] response = readResponse(is, null);
		return new String(response, lpdCharset);
	}

	public String getLongQueueState(OutputStream os, InputStream is,
			String queue, String jobs) throws IOException {

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

		os.write(CMD_SEND_QUEUE_STATE_LONG);
		os.write(queue.getBytes(lpdCharset));
		if (jobs != null) {
			os.write(LPD_WHITESPACE);
			os.write(jobs.getBytes(lpdCharset));
		}
		os.write(LPD_LF);
		os.flush();
		byte[] response = readResponse(is, null);
		return new String(response, lpdCharset);
	}

	public void removeJobs(OutputStream os, String queue, String jobs)
			throws IOException {

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

		os.write(CMD_REMOVE_JOBS);
		os.write(queue.getBytes(lpdCharset));
		os.write(LPD_WHITESPACE);
		os.write(user.getBytes(lpdCharset));
		if (jobs != null) {
			os.write(LPD_WHITESPACE);
			os.write(jobs.getBytes(lpdCharset));
		}
		os.write(LPD_LF);
		os.flush();
	}

	private byte[] readResponse(InputStream is, Integer count)
			throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		if (count == null) {
			while (true) {
				int b = is.read();
				if (b == -1) {
					break;
				}
				bos.write(b);
			}
		} else {
			for (int i = 0; i < count; i++) {
				int b = is.read();
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
