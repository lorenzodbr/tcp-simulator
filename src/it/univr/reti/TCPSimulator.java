package it.univr.reti;

import java.text.DecimalFormat;

public class TCPSimulator {
	public static enum SSTRESHValue {
		INITIAL_RCVWND,						// used if sstresh_0 = rcvwnd_0
		HALF_INITIAL_RCVWND; 				// used if ssthresh_0 = 0.5rcvwnd_0
	}

	public static enum RTOValue {
		DOUBLE_RTT, 						// used if rto = 2rtt
	}

	// PROBLEM DATA
	private int data; 						// in segments
	private final int mssBytes; 			// in bytes
	private int ssthresh; 					// in segments
	private final double rtt; 				// in sec
	private final double rto; 				// in sec
	private final double[][] networkDowns; 	// in sec {start, finish}
	private final int[] rcvwnds; 			// at position i, rcvwnd told at time i; assumes rcvwnd cannot be 0

	// AUXILIARY VARIABLES
	private double cwnd = 1; 				// in segments
	private int nextRcvwnd; 				// in segments
	private int sent;						// actual segments sent each time
	private int rtoScaleFactor = 1; 		// multiplicative factor of RTO
	private int time = 0; 					// quantum of time

	public TCPSimulator(int mssBytes, int dataBytes, SSTRESHValue sstresh, double[][] networkDowns, double[][] rcvwnds,
			double rtt, RTOValue rto) {
		if (rcvwnds.length < 1)
			throw new IllegalArgumentException("Invalid length of rcvwnds (must be > 0)");
		if (mssBytes <= 0 || dataBytes <= 0 || sstresh == null || rtt <= 0 || rto == null)
			throw new IllegalArgumentException("Invalid values provided");

		this.mssBytes = mssBytes;
		this.data = dataBytes / mssBytes;
		this.networkDowns = networkDowns;
		this.rtt = rtt;
		this.rcvwnds = buildRcvwnds(rcvwnds);
		this.ssthresh = (int) (this.rcvwnds[0] * (sstresh == SSTRESHValue.INITIAL_RCVWND ? 1 : 0.5) / mssBytes);
		this.rto = rtt * (rto == RTOValue.DOUBLE_RTT ? 2 : 1);
		this.nextRcvwnd = getNextRcvwnd();
	}

	public void simulate() {
		printStartOfTransmission();

		while (data >= 0) {
			if (!isNetworkDown()) {
				rtoScaleFactor = 1; 				// if network is not down, restore factor to 1

				sent = Math.min((int) cwnd, data); 	// number of segments to be sent each time

				if (data == 0) { 					// check for final print (when ACK are received and ssthresh is set)
					printStatus();
					break;
				}

				data -= sent; 						// data being sent

				printStatus();

				nextRcvwnd = getNextRcvwnd(); 		// calculation of rcvwnd for the next iteration
				if (cwnd < ssthresh)
					cwnd = Math.min(Math.min(cwnd + sent, ssthresh), nextRcvwnd); 	// calculation of cwnd for the next iteration
				else
					cwnd = Math.min(cwnd + sent / cwnd, nextRcvwnd); 				// based on ssthresh

				time++; 							// next time quantum
			} else {
				sent = Math.min((int) cwnd, data); 	// preparing for printing
				data -= sent; 						// calculating how much data would be left if network wasn't down

				printStatus();

				ssthresh = Math.max(1, ((int) cwnd) / 2); 	// set new ssthresh value after network down
				cwnd = 1; 									// set new cwnd value after network down
				data += sent; 								// restore segments that have not been sent

				System.out.println("[!]\t---> Segments sent at [" + new DecimalFormat("##.#").format(time * rtt)
						+ "] were lost, restoring cwnd " + (rtoScaleFactor > 1 ? ", doubling and " : "and ")
						+ "waiting RTO until [" + new DecimalFormat("##.#").format(time * rtt + rto * rtoScaleFactor)
						+ "]... (" + rtoScaleFactor + "x base RTO)");

				time += rto / rtt * rtoScaleFactor; 		// wait rto
				rtoScaleFactor *= 2; 						// double rto for the next time (if any)

				if (rtoScaleFactor == 8) { 					// rto doubling limit check
					System.out.println("[!]\t---> Reached maximum RTO (4x base RTO) and timed out. "
						+ "Connection closed at [" + time + "]");
					return;
				}
			}
		}

		printEndOfTransmission();
	}

	private boolean isNetworkDown() {
		for (int i = 0; i < networkDowns.length; i++) {
			// check two cases:
			// - time is exactly in a network down interval
			// - time is less than a rtt from a network down (hence segments are supposed to be lost during sending)
			if (time * rtt >= networkDowns[i][0] && time * rtt < networkDowns[i][1]
			 	|| Math.abs(time * rtt - networkDowns[i][0]) < rtt)
				return true;
		}

		return false;
	}

	private int getNextRcvwnd() {
		for (int i = time; i > 0; i--) {
			// get the last known rcvwnd
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

	private void printStartOfTransmission() {
		System.out.println("\t----- SOT -----");
	}

	private void printEndOfTransmission() {
		System.out.println("\t----- EOT -----");
	}

	private void printStatus() {
		System.out.print(this);
	}

	public String toString() {
		return "[" + new DecimalFormat("##.#").format(time * rtt) + "]\tremaining = " + data +
				"\t   sent = " + sent +
				"\t   cwnd = " + new DecimalFormat("##.##").format(cwnd) +
				"\tssthresh = " + ssthresh +
				"\t  rcvwnd = " + nextRcvwnd;
	}
}
