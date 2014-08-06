package open.lpd;

import java.io.IOException;
import java.io.InputStream;

import open.lpd.server.IPrintJobQueue;

public class TestQueue implements IPrintJobQueue {

	private boolean sendQueueStateShortFired;
	private boolean sendQueueStateLongFired;
	private boolean removeJobsFired;
	private boolean receiveDataFileFired;
	private boolean receiveControlFileFired;
	private boolean receiveAPrinterJobFired;
	private boolean printAnyWaitingJobsFired;
	private boolean finishedReceivingAPrinterJobFired;
	private boolean abortJobFired;
	private int invocationCount;
	private Boolean dataFirst;

	public TestQueue() {
		this.invocationCount = 0;
		dataFirst = null;
	}

	public int getInvocationCount() {
		return invocationCount;
	}

	public boolean sendQueueStateShortHasFired() {
		return sendQueueStateShortFired;
	}

	public boolean sendQueueStateLongHasFired() {
		return sendQueueStateLongFired;
	}

	public boolean removeJobsHasFired() {
		return removeJobsFired;
	}

	public boolean receiveDataFileHasFired() {
		return receiveDataFileFired;
	}

	public boolean receiveControlFileHasFired() {
		return receiveControlFileFired;
	}

	public boolean receiveAPrinterJobHasFired() {
		return receiveAPrinterJobFired;
	}

	public boolean printAnyWaitingJobsHasFired() {
		return printAnyWaitingJobsFired;
	}

	public boolean finishedReceivingAPrinterJobHasFired() {
		return finishedReceivingAPrinterJobFired;
	}

	public boolean abortJobHasFired() {
		return abortJobFired;
	}

	public boolean isDataFirst() {
		return dataFirst;
	}

	@Override
	public String sendQueueStateShort(String queue, String[] list)
			throws IOException {
		invocationCount++;
		sendQueueStateShortFired = true;
		return TestConstants.SHORT_QUEUE_STATE;
	}

	@Override
	public String sendQueueStateLong(String queue, String[] list)
			throws IOException {
		invocationCount++;
		sendQueueStateLongFired = true;
		return TestConstants.LONG_QUEUE_STATE;
	}

	@Override
	public void removeJobs(String queue, String agent, String[] list)
			throws IOException {
		invocationCount++;
		removeJobsFired = true;
	}

	@Override
	public byte receiveDataFile(int count, String name,
			InputStream clientInStream) throws IOException {
		if (dataFirst == null) {
			dataFirst = true;
		}
		invocationCount++;
		receiveDataFileFired = true;
		clientInStream.skip(count);
		return 0;
	}

	@Override
	public byte receiveControlFile(int count, String name,
			InputStream clientInStream) throws IOException {
		if (dataFirst == null) {
			dataFirst = false;
		}
		invocationCount++;
		receiveControlFileFired = true;
		clientInStream.skip(count);
		return 0;
	}

	@Override
	public byte receiveAPrinterJob(String queue) throws IOException {
		invocationCount++;
		receiveAPrinterJobFired = true;
		return 0;
	}

	@Override
	public void printAnyWaitingJobs(String queue) throws IOException {
		invocationCount++;
		printAnyWaitingJobsFired = true;
	}

	@Override
	public void finishedReceivingAPrinterJob() throws IOException {
		invocationCount++;
		finishedReceivingAPrinterJobFired = true;
	}

	@Override
	public void abortJob() throws IOException {
		invocationCount++;
		abortJobFired = true;
	}
}
