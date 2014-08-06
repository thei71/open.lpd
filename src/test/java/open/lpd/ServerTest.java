package open.lpd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import open.lpd.client.LpdClientProtocol;
import open.lpd.server.LpdServerProtocol;

import org.junit.Assert;
import org.junit.Test;

public class ServerTest {

	private LpdServerProtocol createServerProtocol(InputStream is,
			OutputStream os) {
		LpdServerProtocol serverProtocol = new LpdServerProtocol(is, os,
				new TestQueue());
		return serverProtocol;
	}

	@Test
	public void testServerProtocolCmd1() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		LpdServerProtocol serverProtocol = createServerProtocol(
				new ByteArrayInputStream(
						TestConstants.PRINT_ANY_WAITING_JOBS
								.getBytes(TestConstants.CHARSET)), bos);
		serverProtocol.setCharset(TestConstants.CHARSET);
		serverProtocol.handle();

		// check response

		Assert.assertEquals(TestConstants.NO_RESPONSE,
				bos.toString(TestConstants.CHARSET));

		// check queue method has fired

		TestQueue testQueue = (TestQueue) serverProtocol.getQueue();
		Assert.assertEquals(1, testQueue.getInvocationCount());
		Assert.assertTrue(testQueue.printAnyWaitingJobsHasFired());
	}

	@Test
	public void testServerProtocolCmd2() throws IOException {
		String controlData = "J" + TestConstants.JOB + LpdClientProtocol.LPD_LF;
		String controlDataFile = TestConstants.SEND_FILE.replace(
				TestConstants.CONTROL_DATA_LENGTH,
				String.valueOf(controlData.length())).replace(
				TestConstants.CONTROL_DATA_PATTERN, controlData);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		LpdServerProtocol serverProtocol = createServerProtocol(
				new ByteArrayInputStream(
						controlDataFile.getBytes(TestConstants.CHARSET)), bos);
		serverProtocol.setCharset(TestConstants.CHARSET);
		serverProtocol.handle();

		// check response

		Assert.assertEquals(TestConstants.ACK_STREAM,
				bos.toString(TestConstants.CHARSET));

		// check queue method has fired

		TestQueue testQueue = (TestQueue) serverProtocol.getQueue();
		Assert.assertEquals(4, testQueue.getInvocationCount());
		Assert.assertTrue(testQueue.receiveAPrinterJobHasFired());
		Assert.assertTrue(testQueue.receiveControlFileHasFired());
		Assert.assertTrue(testQueue.receiveDataFileHasFired());
		Assert.assertTrue(testQueue.finishedReceivingAPrinterJobHasFired());
		Assert.assertFalse(testQueue.isDataFirst());
	}

	@Test
	public void testServerProtocolCmd2DataFirst() throws IOException {
		String controlData = "J" + TestConstants.JOB + LpdClientProtocol.LPD_LF;
		String controlDataFile = TestConstants.SEND_FILE_DATA_FIRST.replace(
				TestConstants.CONTROL_DATA_LENGTH,
				String.valueOf(controlData.length())).replace(
				TestConstants.CONTROL_DATA_PATTERN, controlData);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		LpdServerProtocol serverProtocol = createServerProtocol(
				new ByteArrayInputStream(
						controlDataFile.getBytes(TestConstants.CHARSET)), bos);
		serverProtocol.setCharset(TestConstants.CHARSET);
		serverProtocol.handle();

		// check response

		Assert.assertEquals(TestConstants.ACK_STREAM,
				bos.toString(TestConstants.CHARSET));

		// check queue method has fired

		TestQueue testQueue = (TestQueue) serverProtocol.getQueue();
		Assert.assertEquals(4, testQueue.getInvocationCount());
		Assert.assertTrue(testQueue.receiveAPrinterJobHasFired());
		Assert.assertTrue(testQueue.receiveControlFileHasFired());
		Assert.assertTrue(testQueue.receiveDataFileHasFired());
		Assert.assertTrue(testQueue.finishedReceivingAPrinterJobHasFired());
		Assert.assertTrue(testQueue.isDataFirst());
	}

	@Test
	public void testServerProtocolCmd2Abort() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		LpdServerProtocol serverProtocol = createServerProtocol(
				new ByteArrayInputStream(TestConstants.ABORT_JOB
						.getBytes(TestConstants.CHARSET)),
				bos);
		serverProtocol.setCharset(TestConstants.CHARSET);
		serverProtocol.handle();

		// check response

		Assert.assertEquals(TestConstants.SINGLE_ACK_STREAM,
				bos.toString(TestConstants.CHARSET));

		// check queue method has fired

		TestQueue testQueue = (TestQueue) serverProtocol.getQueue();
		Assert.assertEquals(3, testQueue.getInvocationCount());
		Assert.assertTrue(testQueue.receiveAPrinterJobHasFired());
		Assert.assertTrue(testQueue.abortJobHasFired());
		Assert.assertTrue(testQueue.finishedReceivingAPrinterJobHasFired());
	}

	@Test
	public void testServerProtocolCmd3() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		LpdServerProtocol serverProtocol = createServerProtocol(
				new ByteArrayInputStream(
						TestConstants.GET_SHORT_QUEUE_STATE
								.getBytes(TestConstants.CHARSET)), bos);
		serverProtocol.setCharset(TestConstants.CHARSET);
		serverProtocol.handle();

		// check response

		Assert.assertEquals(TestConstants.SHORT_QUEUE_STATE,
				bos.toString(TestConstants.CHARSET));

		// check queue method has fired

		TestQueue testQueue = (TestQueue) serverProtocol.getQueue();
		Assert.assertEquals(1, testQueue.getInvocationCount());
		Assert.assertTrue(testQueue.sendQueueStateShortHasFired());
	}

	@Test
	public void testServerProtocolCmd4() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		LpdServerProtocol serverProtocol = createServerProtocol(
				new ByteArrayInputStream(
						TestConstants.GET_LONG_QUEUE_STATE
								.getBytes(TestConstants.CHARSET)), bos);
		serverProtocol.setCharset(TestConstants.CHARSET);
		serverProtocol.handle();

		// check response

		Assert.assertEquals(TestConstants.LONG_QUEUE_STATE,
				bos.toString(TestConstants.CHARSET));

		// check queue method has fired

		TestQueue testQueue = (TestQueue) serverProtocol.getQueue();
		Assert.assertEquals(1, testQueue.getInvocationCount());
		Assert.assertTrue(testQueue.sendQueueStateLongHasFired());
	}

	@Test
	public void testServerProtocolCmd5() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		LpdServerProtocol serverProtocol = createServerProtocol(
				new ByteArrayInputStream(
						TestConstants.REMOVE_JOBS
								.getBytes(TestConstants.CHARSET)), bos);
		serverProtocol.setCharset(TestConstants.CHARSET);
		serverProtocol.handle();

		// check response

		Assert.assertEquals(TestConstants.NO_RESPONSE,
				bos.toString(TestConstants.CHARSET));

		// check queue method has fired

		TestQueue testQueue = (TestQueue) serverProtocol.getQueue();
		Assert.assertEquals(1, testQueue.getInvocationCount());
		Assert.assertTrue(testQueue.removeJobsHasFired());
	}
}
