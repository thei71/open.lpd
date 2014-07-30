package open.lpd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
		serverProtocol.handle();
		Assert.assertEquals("", bos.toString(TestConstants.CHARSET));
		TestQueue testQueue = (TestQueue) serverProtocol.getQueue();
		Assert.assertEquals(1, testQueue.getInvocationCount());
		Assert.assertTrue(testQueue.printAnyWaitingJobsHasFired());
	}
}
