package open.lpd;

import open.lpd.client.LpdClientProtocol;

public class TestConstants {

	public static final String CHARSET = "iso-8859-1";
	public static final String QUEUE = "testäöüÄÖÜß";
	public static final String AGENT = "testagentäöüÄÖÜß";
	public static final String JOB = "testjobäöüÄÖÜß";
	public static final String JOBS = "test1äöüÄÖÜß,test2äöüÄÖÜß";
	public static final String SHORT_QUEUE_STATE = "test1äöüÄÖÜß"
			+ LpdClientProtocol.LPD_LF + "test2äöüÄÖÜß";
	public static final String LONG_QUEUE_STATE = "test1äöüÄÖÜß\t51 byte\tuser"
			+ LpdClientProtocol.LPD_LF + "test2äöüÄÖÜß\t33 byte\tuser";
	public static final String DATA = "Printed Data, öäüÖÄÜß.";
	public static final String CLIENT_HOST = "testhostäöüÄÖÜß";

	public static final String PRINT_ANY_WAITING_JOBS = "\u0001"
			+ QUEUE + LpdClientProtocol.LPD_LF;
	public static final String GET_SHORT_QUEUE_STATE = "\u0003"
			+ QUEUE + LpdClientProtocol.LPD_WHITESPACE + JOBS
			+ LpdClientProtocol.LPD_LF;
	public static final String GET_LONG_QUEUE_STATE = "\u0004"
			+ QUEUE + LpdClientProtocol.LPD_WHITESPACE + JOBS
			+ LpdClientProtocol.LPD_LF;
	public static final String REMOVE_JOBS = "\u0005" + QUEUE
			+ LpdClientProtocol.LPD_WHITESPACE + AGENT
			+ LpdClientProtocol.LPD_WHITESPACE + JOBS
			+ LpdClientProtocol.LPD_LF;
	public static final String SEND_FILE = "\u0002" + QUEUE
			+ LpdClientProtocol.LPD_LF + "\u0002\\d+"
			+ LpdClientProtocol.LPD_WHITESPACE + "cfA000" + CLIENT_HOST
			+ LpdClientProtocol.LPD_LF + "([^\u0000]+)\u0000\u0003"
			+ DATA.length() + LpdClientProtocol.LPD_WHITESPACE + "dfA000"
			+ CLIENT_HOST + LpdClientProtocol.LPD_LF + DATA
			+ "\u0000";
	public static final String SEND_FILE_DATA_FIRST = "\u0002"
			+ QUEUE + LpdClientProtocol.LPD_LF + "\u0003"
			+ DATA.length() + LpdClientProtocol.LPD_WHITESPACE + "dfA000"
			+ CLIENT_HOST + LpdClientProtocol.LPD_LF + DATA
			+ "\u0000\u0002\\d+" + LpdClientProtocol.LPD_WHITESPACE + "cfA000"
			+ CLIENT_HOST + LpdClientProtocol.LPD_LF
			+ "([^\u0000]+)\u0000";
}
