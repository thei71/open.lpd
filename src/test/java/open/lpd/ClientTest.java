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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

import open.lpd.client.LpdClientProtocol;

import org.junit.Assert;
import org.junit.Test;

public class ClientTest {

	private LpdClientProtocol createClientProtocol(InputStream is,
			OutputStream os) {
		LpdClientProtocol clientProtocol = new LpdClientProtocol(is, os);
		clientProtocol.setCharset(TestConstants.CHARSET);
		clientProtocol.setSendDataFirst(false);
		clientProtocol.setClientHost(TestConstants.CLIENT_HOST);
		clientProtocol.setUser(TestConstants.AGENT);
		return clientProtocol;
	}

	@Test
	public void testClientProtocolCmd1() throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		LpdClientProtocol clientProtocol = createClientProtocol(null, bos);

		clientProtocol.printQueue(TestConstants.QUEUE);
		Assert.assertEquals(TestConstants.PRINT_ANY_WAITING_JOBS,
				bos.toString(TestConstants.CHARSET));
	}

	@Test
	public void testClientProtocolCmd2() throws IOException {

		byte[] ackStream = "\u0000\u0000\u0000\u0000\u0000"
				.getBytes(TestConstants.CHARSET);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		LpdClientProtocol clientProtocol = createClientProtocol(
				new ByteArrayInputStream(ackStream), bos);

		byte[] testData = TestConstants.DATA
				.getBytes(TestConstants.CHARSET);
		clientProtocol.sendFile(TestConstants.QUEUE,
				TestConstants.JOB,
				new ByteArrayInputStream(testData), testData.length);
		Assert.assertTrue(Pattern.matches(TestConstants.SEND_FILE,
				bos.toString(TestConstants.CHARSET)));
	}

	@Test
	public void testClientProtocolCmd2DataFirst() throws IOException {

		byte[] ackStream = "\u0000\u0000\u0000\u0000\u0000"
				.getBytes(TestConstants.CHARSET);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		LpdClientProtocol clientProtocol = createClientProtocol(
				new ByteArrayInputStream(ackStream), bos);
		clientProtocol.setSendDataFirst(true);

		byte[] testData = TestConstants.DATA
				.getBytes(TestConstants.CHARSET);
		clientProtocol.sendFile(TestConstants.QUEUE,
				TestConstants.JOB,
				new ByteArrayInputStream(testData), testData.length);
		Assert.assertTrue(Pattern.matches(
				TestConstants.SEND_FILE_DATA_FIRST,
				bos.toString(TestConstants.CHARSET)));
	}

	@Test
	public void testClientProtocolCmd3() throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		LpdClientProtocol clientProtocol = createClientProtocol(
				new ByteArrayInputStream(
						TestConstants.SHORT_QUEUE_STATE
								.getBytes(TestConstants.CHARSET)), bos);

		String queueState = clientProtocol.getShortQueueState(
				TestConstants.QUEUE, TestConstants.JOBS);
		Assert.assertEquals(TestConstants.GET_SHORT_QUEUE_STATE,
				bos.toString(TestConstants.CHARSET));
		Assert.assertEquals(TestConstants.SHORT_QUEUE_STATE, queueState);
	}

	@Test
	public void testClientProtocolCmd4() throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		LpdClientProtocol clientProtocol = createClientProtocol(
				new ByteArrayInputStream(
						TestConstants.LONG_QUEUE_STATE
								.getBytes(TestConstants.CHARSET)), bos);

		String queueState = clientProtocol.getLongQueueState(
				TestConstants.QUEUE, TestConstants.JOBS);
		Assert.assertEquals(TestConstants.GET_LONG_QUEUE_STATE,
				bos.toString(TestConstants.CHARSET));
		Assert.assertEquals(TestConstants.LONG_QUEUE_STATE, queueState);
	}

	@Test
	public void testClientProtocolCmd5() throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		LpdClientProtocol clientProtocol = createClientProtocol(null, bos);

		clientProtocol.removeJobs(TestConstants.QUEUE,
				TestConstants.JOBS);
		Assert.assertEquals(TestConstants.REMOVE_JOBS,
				bos.toString(TestConstants.CHARSET));
	}
}
