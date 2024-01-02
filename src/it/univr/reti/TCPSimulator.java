package it.univr.reti;

import java.text.DecimalFormat;

public class TCPSimulator {
	// CONSTANTS
	public static int INITIAL_RCVWND = 1; // in segments
	public static int HALF_INITIAL_RCVWND = 2; // ratio rcvwnd / ssthresh
	public static int DOUBLE_RTT = 2; // ratio rto / rtt
	public static final double[][] NO_NETWORK_DOWNS = {}; // no network downs

	private static final int MAX_RTO = 8; // maximum rto scale factor
	private static final int MIN_RTO = 1; // base rto scale factor
	private static final int MIN_CWND = 1; // base cwnd value
	private static final int NO_MORE_DATA = 0; // no more data to send
	private static final int MIN_SSTHRESH = 1; // base ssthresh value
	private static final double GRAPHIC_DESYNC_FACTOR = .997; // desync factor for when two lines overlap

	private static final boolean ADD_CWND = true; // flags used to specify which values to add to the plot
	private static final boolean ADD_SSTHRESH = true;
	private static final boolean ADD_RCVWND = true;
	private static final boolean DO_NOT_ADD_CWND = false;

	// PROBLEM DATA
	private int data; // in segments
	private final int mssBytes; // in bytes
	private int ssthresh; // in segments
	private final double rtt; // in sec
	private final double rto; // times compared to rtt
	private final double[][] networkDowns; // in sec {start, finish}
	private final int[] rcvwnds; // at position i, rcvwnd told at time i; assumes rcvwnd cannot be 0

	// AUXILIARY VARIABLES
	private double cwnd = MIN_CWND; // in segments
	private int currentRcvwnd; // in segments
	private int nextRcvwnd; // in segments
	private int sent; // actual segments sent each time
	private int rtoScaleFactor = MIN_RTO; // multiplicative factor of RTO
	private int time = 0; // quantum of time

	private TCPPlot plot;

	public TCPSimulator(int mssBytes, int dataBytes, int ssthresh, double[][] networkDowns, double[][] rcvwnds,
			double rtt, int rto) throws IllegalArgumentException {
		verify(mssBytes, dataBytes, ssthresh, networkDowns, rcvwnds, rtt, rto);

		this.mssBytes = mssBytes;
		this.data = dataBytes / mssBytes;
		this.networkDowns = networkDowns == null ? NO_NETWORK_DOWNS : networkDowns;
		this.rtt = rtt;
		this.rcvwnds = buildRcvwnds(rcvwnds);
		this.ssthresh = (int) (this.rcvwnds[0] / ssthresh / mssBytes);
		this.rto = rto;
		this.nextRcvwnd = getNextRcvwnd();

		this.plot = new TCPPlot();
	}

	public void simulate() {
		printStartOfTransmission();
		addNetworkDownsToPlot();

		while (data >= NO_MORE_DATA) {
			sent = Math.min((int) cwnd, data); // number of segments to be sent each time

			if (!isNetworkDown()) {
				addPointsToPlot(ADD_CWND, ADD_SSTHRESH, ADD_RCVWND);

				rtoScaleFactor = MIN_RTO; // if network is not down, restore factor to 1

				if (data == NO_MORE_DATA) { // check for final print (when ACK are received and ssthresh is set)
					printStatus();
					break;
				}

				data -= sent; // data being sent

				printStatus();

				currentRcvwnd = nextRcvwnd; // rcvwnd at the beginning of the iteration
				nextRcvwnd = getNextRcvwnd(); // calculation of rcvwnd for the next iteration

				if (currentRcvwnd != nextRcvwnd) { // if rcvwnd changed, add the old to plot to have straight lines
					plot.addPointToPlot(TCPPlot.RCVWND_LABEL, (time + 1) * rtt, currentRcvwnd);
				}

				if (cwnd < ssthresh)
					cwnd = Math.min(Math.min(cwnd + sent, ssthresh), nextRcvwnd); // calculation of cwnd for the next
																					// iteration
				else
					cwnd = Math.min(cwnd + sent / cwnd, nextRcvwnd); // based on ssthresh

				time++; // next time quantum
			} else {
				data -= sent; // calculating how much data would be left if network wasn't down

				printStatus();

				plot.addPointToPlot(TCPPlot.SSTHRESH_LABEL, // add ssthresh to plot to have straight lines
						(time + rto * rtoScaleFactor) * rtt,
						ssthresh * (nextRcvwnd == ssthresh ? GRAPHIC_DESYNC_FACTOR : 1));
				plot.addPointToPlot(TCPPlot.SEGMENTS_LOST_LABEL, time * rtt, cwnd); // add segments lost to plot

				ssthresh = Math.max(MIN_SSTHRESH, ((int) cwnd) / 2); // set new ssthresh value after network down
				cwnd = MIN_CWND; // set new cwnd value after network down
				data += sent; // restore segments that have not been lost

				printSegmentLoss();

				time += rto * rtoScaleFactor; // wait rto
				rtoScaleFactor *= 2; // double rto for the next time (if any)

				if (rtoScaleFactor == MAX_RTO) { // rto doubling limit check
					printTimeOut();

					// add timeout symbol to plot
					plot.addPointToPlot(TCPPlot.CONNECTION_TIMED_OUT_LABEL, time * rtt, ssthresh);
					break;
				}

				addPointsToPlot(DO_NOT_ADD_CWND, ADD_SSTHRESH, ADD_RCVWND);
			}
		}

		printEndOfTransmission();
		addPointsToPlot(rtoScaleFactor == MAX_RTO ? DO_NOT_ADD_CWND : ADD_CWND, ADD_SSTHRESH, ADD_RCVWND);
		showPlot();
	}

	private boolean isNetworkDown() {
		for (double[] networkDown : networkDowns) {
			double start = networkDown[0];
			double finish = networkDown[1];

			// check two cases:
			// - time is exactly in a network down interval
			// - time is less than a rtt from a network down (hence segments are supposed to
			// be lost during sending)
			if (time * rtt >= start && time * rtt < finish || Math.abs(time * rtt - start) < rtt) {
				return true;
			}
		}

		return false;
	}

	private int getNextRcvwnd() {
		for (int i = time; i > 0; i--) {
			// get the last known rcvwnd by looking backwards in time
			if (i < rcvwnds.length && rcvwnds[i] != 0)
				return rcvwnds[i] / mssBytes;
		}

		return rcvwnds[0] / mssBytes;
	}

	private int[] buildRcvwnds(double[]... timesAndValues) {
		double max = 0;

		// get the maximum time so the array can be built with max / rtt + 1 cells
		for (double[] timeAndValue : timesAndValues) {
			if (timeAndValue[0] > max) {
				max = timeAndValue[0];
			}
		}

		int[] arr = new int[(int) (max / rtt) + 1];

		// loading values at given position, scaling with rtt
		for (double[] timeAndValue : timesAndValues) {
			arr[(int) (timeAndValue[0] / rtt)] = (int) timeAndValue[1];
		}

		return arr;
	}

	private void verify(int mssBytes, int dataBytes, int ssthresh, double[][] networkDowns, double[][] rcvwnds,
			double rtt, int rto) {
		// check for null values
		if (rcvwnds == null || rcvwnds.length < 1)
			throw new IllegalArgumentException("Invalid length of rcvwnds (must be > 0)");
		if (mssBytes <= 0 || dataBytes <= 0 || ssthresh <= 0 || rtt <= 0 || rto <= 0)
			throw new IllegalArgumentException("Invalid values provided");

		// check for negative or overlapping rcvwnds
		for (int i = 0; i < rcvwnds.length; i++) {
			if (rcvwnds[i][0] < 0 || rcvwnds[i][1] <= 0)
				throw new IllegalArgumentException("Invalid values provided");

			for (int j = i - 1; j >= 0; j--) {
				if (rcvwnds[i][0] <= rcvwnds[j][0])
					throw new IllegalArgumentException("Overlapping rcvwnds provided");
			}
		}

		// check for negative or overlapping network downs
		for (int i = 0; i < networkDowns.length; i++) {
			if (networkDowns[i][0] < 0 || networkDowns[i][1] <= 0 || networkDowns[i][0] >= networkDowns[i][1])
				throw new IllegalArgumentException("Invalid values provided");

			for (int j = i + 1; j < networkDowns.length; j++) {
				if (networkDowns[i][0] >= networkDowns[j][0] && networkDowns[i][0] < networkDowns[j][1]
						|| networkDowns[i][1] > networkDowns[j][0] && networkDowns[i][1] <= networkDowns[j][1])
					throw new IllegalArgumentException("Overlapping network downs provided");
			}
		}

		// check for invalid values of ssthresh and rto
		if (ssthresh != INITIAL_RCVWND && ssthresh != HALF_INITIAL_RCVWND)
			throw new IllegalArgumentException("Invalid value provided for SSTHRESH");

		if (rto != DOUBLE_RTT)
			throw new IllegalArgumentException("Invalid value provided for RTO");
	}

	private void showPlot() {
		plot.showPlot();
	}

	private void addPointsToPlot(boolean addCwnd, boolean addSsthresh, boolean addRcvwnd) {
		if (addCwnd)
			plot.addPointToPlot(TCPPlot.CWND_LABEL, time * rtt, cwnd); // add cwnd to plot
		if (addRcvwnd)
			plot.addPointToPlot(TCPPlot.RCVWND_LABEL, time * rtt, nextRcvwnd); // add rcvwnd to plot
		if (addSsthresh)
			plot.addPointToPlot(TCPPlot.SSTHRESH_LABEL, time * rtt,
					ssthresh * (nextRcvwnd == ssthresh ? GRAPHIC_DESYNC_FACTOR : 1));
	}

	private void addNetworkDownsToPlot() {
		for (int i = 0; i < networkDowns.length; i++) {
			plot.addNetworkDownToPlot(i, networkDowns[i]); // add network downs to plot
		}
	}

	private void printStartOfTransmission() {
		System.out.println("\t----- SOT -----");
	}

	private void printEndOfTransmission() {
		System.out.println("\t----- EOT -----");
	}

	private void printTimeOut() {
		System.out.println("[!]\t---> Reached maximum RTO (" + MAX_RTO / 2 + "x base RTO) and timed out. "
				+ "Connection closed at [" + time + "]");
	}

	private void printSegmentLoss() {
		DecimalFormat f = new DecimalFormat("##.#");
		System.out.println("[!]\t---> Segments sent at [" + f.format(time * rtt)
				+ "] were lost, restoring cwnd " + (rtoScaleFactor > MIN_RTO ? ", doubling and " : "and ")
				+ "waiting RTO until [" + f.format(rtt * (time + rto * rtoScaleFactor))
				+ "]... (" + rtoScaleFactor + "x base RTO)");
	}

	private void printStatus() {
		System.out.println(this);
	}

	public String toString() {
		DecimalFormat f = new DecimalFormat("##.#");
		return "[" + f.format(time * rtt) + "]\tremaining = " + data +
				"\t   sent = " + sent +
				"\t   cwnd = " + f.format(cwnd) +
				"\tssthresh = " + ssthresh +
				"\t  rcvwnd = " + nextRcvwnd;
	}
}
