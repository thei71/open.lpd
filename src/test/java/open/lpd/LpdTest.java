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
package open.lpd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import open.lpd.client.LpdClientProtocol;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LpdTest {

	private static final String TEST_CHARSET = "iso-8859-1";
	private static final String TEST_QUEUE = "testäöüÄÖÜß";
	private static final String TEST_AGENT = "testagentäöüÄÖÜß";
	private static final String TEST_JOB_NAME = "testjobäöüÄÖÜß";
	private static final String TEST_JOBS = "test1äöüÄÖÜß,test2äöüÄÖÜß";
	private static final String TEST_SHORT_QUEUE_STATE = "test1äöüÄÖÜß"
			+ LpdClientProtocol.LPD_LF + "test2äöüÄÖÜß";
	private static final String TEST_LONG_QUEUE_STATE = "test1äöüÄÖÜß\t51 byte\tuser"
			+ LpdClientProtocol.LPD_LF + "test2äöüÄÖÜß\t33 byte\tuser";
	private static final String TEST_DATA = "Printed Data, öäüÖÄÜß.";
	private static final String TEST_CLIENT_HOST = "testhostäöüÄÖÜß";
	private static final String TEST_CONTROL_FILE = "H" + TEST_CLIENT_HOST
			+ LpdClientProtocol.LPD_LF + "J" + TEST_JOB_NAME
			+ LpdClientProtocol.LPD_LF + "N" + TEST_JOB_NAME
			+ LpdClientProtocol.LPD_LF + "P" + TEST_AGENT
			+ LpdClientProtocol.LPD_LF;

	private static final String PROTOCOL_PRINT_ANY_WAITING_JOBS = "\u0001"
			+ TEST_QUEUE + LpdClientProtocol.LPD_LF;
	private static final String PROTOCOL_GET_SHORT_QUEUE_STATE = "\u0003"
			+ TEST_QUEUE + LpdClientProtocol.LPD_WHITESPACE + TEST_JOBS
			+ LpdClientProtocol.LPD_LF;
	private static final String PROTOCOL_GET_LONG_QUEUE_STATE = "\u0004"
			+ TEST_QUEUE + LpdClientProtocol.LPD_WHITESPACE + TEST_JOBS
			+ LpdClientProtocol.LPD_LF;
	private static final String PROTOCOL_REMOVE_JOBS = "\u0005" + TEST_QUEUE
			+ LpdClientProtocol.LPD_WHITESPACE + TEST_AGENT
			+ LpdClientProtocol.LPD_WHITESPACE + TEST_JOBS
			+ LpdClientProtocol.LPD_LF;
	private static final String PROTOCOL_SEND_FILE = "\u0002" + TEST_QUEUE
			+ LpdClientProtocol.LPD_LF + "\u0002" + TEST_CONTROL_FILE.length()
			+ LpdClientProtocol.LPD_WHITESPACE + "cfA000" + TEST_CLIENT_HOST
			+ LpdClientProtocol.LPD_LF + TEST_CONTROL_FILE + "\u0000\u0003"
			+ TEST_DATA.length() + LpdClientProtocol.LPD_WHITESPACE + "dfA000"
			+ TEST_CLIENT_HOST + LpdClientProtocol.LPD_LF + TEST_DATA
			+ "\u0000";
	private static final String PROTOCOL_SEND_FILE_DATA_FIRST = "\u0002"
			+ TEST_QUEUE + LpdClientProtocol.LPD_LF + "\u0003"
			+ TEST_DATA.length() + LpdClientProtocol.LPD_WHITESPACE + "dfA000"
			+ TEST_CLIENT_HOST + LpdClientProtocol.LPD_LF + TEST_DATA
			+ "\u0000\u0002" + TEST_CONTROL_FILE.length()
			+ LpdClientProtocol.LPD_WHITESPACE + "cfA000" + TEST_CLIENT_HOST
			+ LpdClientProtocol.LPD_LF + TEST_CONTROL_FILE + "\u0000";

	private LpdClientProtocol clientProtocol;

	@Before
	public void setUp() {
		clientProtocol = new LpdClientProtocol();
		clientProtocol.setCharset(TEST_CHARSET);
		clientProtocol.setSendDataFirst(false);
		clientProtocol.setClientHost(TEST_CLIENT_HOST);
		clientProtocol.setUser(TEST_AGENT);
	}

	@Test
	public void testLpdProtocolCmd1() throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		clientProtocol.printQueue(bos, TEST_QUEUE);
		Assert.assertArrayEquals(
				PROTOCOL_PRINT_ANY_WAITING_JOBS.getBytes(TEST_CHARSET),
				bos.toByteArray());
	}

	@Test
	public void testLpdProtocolCmd2() throws IOException {

		byte[] ackStream = "\u0000\u0000\u0000\u0000\u0000"
				.getBytes(TEST_CHARSET);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] testData = TEST_DATA.getBytes(TEST_CHARSET);
		clientProtocol.sendFile(bos, new ByteArrayInputStream(ackStream),
				TEST_QUEUE, TEST_JOB_NAME, new ByteArrayInputStream(testData),
				testData.length);
		Assert.assertArrayEquals(PROTOCOL_SEND_FILE.getBytes(TEST_CHARSET),
				bos.toByteArray());
	}

	@Test
	public void testLpdProtocolCmd2DataFirst() throws IOException {

		clientProtocol.setSendDataFirst(true);
		byte[] ackStream = "\u0000\u0000\u0000\u0000\u0000"
				.getBytes(TEST_CHARSET);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] testData = TEST_DATA.getBytes(TEST_CHARSET);
		clientProtocol.sendFile(bos, new ByteArrayInputStream(ackStream),
				TEST_QUEUE, TEST_JOB_NAME, new ByteArrayInputStream(testData),
				testData.length);
		Assert.assertArrayEquals(
				PROTOCOL_SEND_FILE_DATA_FIRST.getBytes(TEST_CHARSET),
				bos.toByteArray());
	}

	@Test
	public void testLpdProtocolCmd3() throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		String queueState = clientProtocol.getShortQueueState(
				bos,
				new ByteArrayInputStream(TEST_SHORT_QUEUE_STATE
						.getBytes(TEST_CHARSET)), TEST_QUEUE, TEST_JOBS);
		Assert.assertArrayEquals(
				PROTOCOL_GET_SHORT_QUEUE_STATE.getBytes(TEST_CHARSET),
				bos.toByteArray());
		Assert.assertEquals(TEST_SHORT_QUEUE_STATE, queueState);
	}

	@Test
	public void testLpdProtocolCmd4() throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		String queueState = clientProtocol.getLongQueueState(
				bos,
				new ByteArrayInputStream(TEST_LONG_QUEUE_STATE
						.getBytes(TEST_CHARSET)), TEST_QUEUE, TEST_JOBS);
		Assert.assertArrayEquals(
				PROTOCOL_GET_LONG_QUEUE_STATE.getBytes(TEST_CHARSET),
				bos.toByteArray());
		Assert.assertEquals(TEST_LONG_QUEUE_STATE, queueState);
	}

	@Test
	public void testLpdProtocolCmd5() throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		clientProtocol.removeJobs(bos, TEST_QUEUE, TEST_JOBS);
		Assert.assertArrayEquals(PROTOCOL_REMOVE_JOBS.getBytes(TEST_CHARSET),
				bos.toByteArray());
	}
}
