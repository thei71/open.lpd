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

import open.lpd.server.impl.FileBasedPrintJobQueue;

/**
 * LPD queue interface for implementing a LPD protocol server. A file based
 * sample implementation is provided, {@link FileBasedPrintJobQueue}.
 */
public interface IPrintJobQueue {

	/**
	 * Prints any waiting jobs for the specified queue.
	 * 
	 * @param queue
	 *            the name of the queue.
	 * 
	 * @throws IOException
	 *             throws if there was an input output error.
	 */
	void printAnyWaitingJobs(String queue) throws IOException;

	/**
	 * Receives a printer job for the specified queue.
	 * 
	 * @param queue
	 *            the name of the queue.
	 * @return a code indicating success, {@link LpdServerProtocol#ACK_SUCCESS},
	 *         or an error code (any other value) indicating failure.
	 * @throws IOException
	 *             throws if there was an input output error.
	 */
	byte receiveAPrinterJob(String queue) throws IOException;

	/**
	 * Sends the short state of selected print jobs for the specified queue.
	 * 
	 * @param queue
	 *            the name of the queue.
	 * @param list
	 *            the list of print jobs.
	 * @return a code indicating success, {@link LpdServerProtocol#ACK_SUCCESS},
	 *         or an error code (any other value) indicating failure.
	 * @throws IOException
	 *             throws if there was an input output error.
	 */
	String sendQueueStateShort(String queue, String[] list) throws IOException;

	/**
	 * Sends the long state of selected print jobs for the specified queue.
	 * 
	 * @param queue
	 *            the name of the queue.
	 * @param list
	 *            the list of print jobs.
	 * @return a code indicating success, {@link LpdServerProtocol#ACK_SUCCESS},
	 *         or an error code (any other value) indicating failure.
	 * @throws IOException
	 *             throws if there was an input output error.
	 */
	String sendQueueStateLong(String queue, String[] list) throws IOException;

	/**
	 * Removes selected print jobs for the specified queue.
	 * 
	 * @param queue
	 *            the name of the queue.
	 * @param agent
	 * @param list
	 *            the list of print jobs.
	 * @throws IOException
	 *             throws if there was an input output error.
	 */
	void removeJobs(String queue, String agent, String[] list)
			throws IOException;

	/**
	 * Aborts the current print job.
	 * 
	 * @throws IOException
	 *             throws if there was an input output error.
	 */
	void abortJob() throws IOException;

	/**
	 * Receives a control file for the current print job.
	 * 
	 * @param count
	 *            the number of bytes to receive.
	 * @param name
	 *            the name of the control file.
	 * @param clientInStream
	 *            the input stream to read the control file from.
	 * @return a code indicating success, {@link LpdServerProtocol#ACK_SUCCESS},
	 *         or an error code (any other value) indicating failure.
	 * @throws IOException
	 *             throws if there was an input output error.
	 */
	byte receiveControlFile(int count, String name, InputStream clientInStream)
			throws IOException;

	/**
	 * Receives a data file for the current print job.
	 * 
	 * @param count
	 *            the number of bytes to receive.
	 * @param name
	 *            the name of the data file.
	 * @param clientInStream
	 *            the input stream to read the data file from.
	 * @return a code indicating success, {@link LpdServerProtocol#ACK_SUCCESS},
	 *         or an error code (any other value) indicating failure.
	 * @throws IOException
	 *             throws if there was an input output error.
	 */
	byte receiveDataFile(int count, String name, InputStream clientInStream)
			throws IOException;

	/**
	 * Indicates that receiving a print job has finished.
	 * 
	 * @throws IOException
	 *             throws if there was an input output error.
	 */
	void finishedReceivingAPrinterJob() throws IOException;
}
