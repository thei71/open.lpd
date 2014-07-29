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
package open.lpd.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Use the LpdServerProtocol class to implement your own LPD server. The
 * LpdServerProtocol uses the {@link IPrintJobQueue} interface as a queue back
 * end to handle print jobs.
 */
public class LpdServerProtocol {

	public static final byte CMD_PRINT_ANY_WAITING_JOBS = 1;
	public static final byte CMD_RECEIVE_A_PRINTER_JOB = 2;
	public static final byte CMD_SEND_QUEUE_STATE_SHORT = 3;
	public static final byte CMD_SEND_QUEUE_STATE_LONG = 4;
	public static final byte CMD_REMOVE_JOBS = 5;
	public static final byte SUB_CMD_ABORT_JOB = 1;
	public static final byte SUB_CMD_RECEIVE_CONTROL_FILE = 2;
	public static final byte SUB_CMD_RECEIVE_DATA_FILE = 3;
	public static final String LPD_DEFAULT_CHARSET = "ASCII";
	public static final byte ACK_SUCCESS = 0;
	private static final String REGEXP_WHITESPACE = "\\s";
	private static final int LPD_LF = 0x0a;

	private IPrintJobQueue printJobQueue;
	private InputStream clientInStream;
	private OutputStream clientOutStream;
	private String protocolCharset = LPD_DEFAULT_CHARSET;

	/**
	 * Creates a LPD server protocol which handles a single client connection.
	 * 
	 * @param clientInStream
	 *            the stream to receive protocol commands from the client.
	 * @param clientOutStream
	 *            the stream to send protocol commands to the client.
	 * @param lpdQueue
	 *            the queue that handles the print jobs of this server.
	 */
	public LpdServerProtocol(InputStream clientInStream,
			OutputStream clientOutStream, IPrintJobQueue lpdQueue) {
		this.clientInStream = clientInStream;
		this.clientOutStream = clientOutStream;
		this.printJobQueue = lpdQueue;
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
	 * Handles client connections.
	 * 
	 * @throws IOException
	 *             throws if an I/O error happens during the protocol.
	 */
	public void handle() throws IOException {

		int cmd = clientInStream.read();
		switch (cmd) {
		case CMD_PRINT_ANY_WAITING_JOBS:
			handlePrintAnyWaitingJobsCommand();
			break;
		case CMD_RECEIVE_A_PRINTER_JOB:
			handleReceiveAPrinterJobCommand();
			boolean moreSubCmdsAvailable = true;
			while (moreSubCmdsAvailable) {
				int subCmd = clientInStream.read();
				switch (subCmd) {
				case SUB_CMD_ABORT_JOB:
					handleAbortJobCommand();
					break;
				case SUB_CMD_RECEIVE_CONTROL_FILE:
					handleReceiveControlFile();
					break;
				case SUB_CMD_RECEIVE_DATA_FILE:
					handleReceiveDataFile();
					break;
				case -1:
					moreSubCmdsAvailable = false;
					break;
				default:
					throw new IOException("unsupported subCmd: " + cmd);
				}
			}
			printJobQueue.finishedReceivingAPrinterJob();
			break;
		case CMD_SEND_QUEUE_STATE_SHORT:
			handleSendQueueStateShortCommand();
			break;
		case CMD_SEND_QUEUE_STATE_LONG:
			handleSendQueueStateLongCommand();
			break;
		case CMD_REMOVE_JOBS:
			handleRemoveJobsCommand();
			break;
		default:
			throw new IOException("unsupported cmd: " + cmd);
		}
	}

	/**
	 * Handles the print any waiting jobs command.
	 * 
	 * @throws IOException
	 *             throws if an I/O error happens during the protocol.
	 */
	private void handlePrintAnyWaitingJobsCommand() throws IOException {

		// +----+-------+----+
		// | 01 | Queue | LF |
		// +----+-------+----+
		// Command code - 1
		// Operand - Printer queue name
		//
		// This command starts the printing process if it not already running.

		String queue = readLine();
		printJobQueue.printAnyWaitingJobs(queue);
	}

	/**
	 * Handles the receive a printer job command.
	 * 
	 * @throws IOException
	 *             throws if an I/O error happens during the protocol.
	 */
	private void handleReceiveAPrinterJobCommand() throws IOException {

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

		String queue = readLine();
		byte code = printJobQueue.receiveAPrinterJob(queue);
		acknowledge(false, code);
	}

	/**
	 * Handles the send queue state short command.
	 * 
	 * @throws IOException
	 *             throws if an I/O error happens during the protocol.
	 */
	private void handleSendQueueStateShortCommand() throws IOException {

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

		String line = readLine();
		String[] lines = line.split(REGEXP_WHITESPACE);
		String queue = lines[0];
		String[] list = (lines.length > 1) ? lines[1].split(REGEXP_WHITESPACE)
				: null;
		String state = printJobQueue.sendQueueStateShort(queue, list);
		clientOutStream.write(state.getBytes(protocolCharset));
		clientOutStream.flush();
	}

	/**
	 * Handles the send queue state long command.
	 * 
	 * @throws IOException
	 *             throws if an I/O error happens during the protocol.
	 */
	private void handleSendQueueStateLongCommand() throws IOException {

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

		String line = readLine();
		String[] lines = line.split(REGEXP_WHITESPACE);
		String queue = lines[0];
		String[] list = (lines.length > 1) ? lines[1].split(REGEXP_WHITESPACE)
				: null;
		String state = printJobQueue.sendQueueStateLong(queue, list);
		clientOutStream.write(state.getBytes(protocolCharset));
		clientOutStream.flush();
	}

	/**
	 * Handles the remove jobs command.
	 * 
	 * @throws IOException
	 *             throws if an I/O error happens during the protocol.
	 */
	private void handleRemoveJobsCommand() throws IOException {

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

		String line = readLine();
		String[] lines = line.split(REGEXP_WHITESPACE);
		String queue = lines[0];
		String agent = lines[1];
		String[] list = (lines.length > 2) ? lines[2].split(REGEXP_WHITESPACE)
				: null;
		printJobQueue.removeJobs(queue, agent, list);
	}

	/**
	 * Handles the abort job sub command.
	 * 
	 * @throws IOException
	 *             throws if an I/O error happens during the protocol.
	 */
	private void handleAbortJobCommand() throws IOException {

		// +----+----+
		// | 01 | LF |
		// +----+----+
		// Command code - 1
		//
		// No operands should be supplied. This subcommand will remove any
		// files which have been created during this "Receive job" command.

		printJobQueue.abortJob();
	}

	/**
	 * Handles the receive control file sub command.
	 * 
	 * @throws IOException
	 *             throws if an I/O error happens during the protocol.
	 */
	private void handleReceiveControlFile() throws IOException {

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

		String line = readLine();
		String[] lines = line.split(REGEXP_WHITESPACE);
		int count = Integer.valueOf(lines[0]);
		String name = lines[1];
		acknowledge(false, ACK_SUCCESS);
		byte code = printJobQueue.receiveControlFile(count, name,
				clientInStream);
		acknowledge(true, code);
	}

	/**
	 * Handles the receive data file sub command.
	 * 
	 * @throws IOException
	 *             throws if an I/O error happens during the protocol.
	 */
	private void handleReceiveDataFile() throws IOException {

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

		String line = readLine();
		String[] lines = line.split(REGEXP_WHITESPACE);
		int count = Integer.valueOf(lines[0]);
		String name = lines[1];
		acknowledge(false, ACK_SUCCESS);
		byte code = printJobQueue.receiveDataFile(count, name, clientInStream);
		acknowledge(true, code);
	}

	/**
	 * Performs a protocol ack.
	 * 
	 * @param receiveAck
	 *            true if an ack is expected from the client.
	 * @param code
	 *            the ack code to send to the client.
	 * 
	 * @throws IOException
	 *             throws if an I/O error happens during the protocol.
	 */
	private void acknowledge(boolean receiveAck, byte code) throws IOException {

		// receive ack code

		if (receiveAck) {
			int ack = clientInStream.read();
			if (ack != 0) {
				throw new IOException("Received invalid ack: " + ack);
			}
		}

		// send ack code

		clientOutStream.write(code);
		clientOutStream.flush();
	}

	/**
	 * Reads a protocol line from the client connection.
	 * 
	 * @return a protocol line.
	 * 
	 * @throws IOException
	 *             throws if an I/O error happens during the protocol.
	 */
	private String readLine() throws IOException {

		StringBuilder sb = new StringBuilder();
		while (true) {
			int c = clientInStream.read();
			if (c == -1) {
				break;
			}
			if (c == LPD_LF) {
				break;
			}
			sb.append((char) c);
		}
		return sb.toString();
	}
}
