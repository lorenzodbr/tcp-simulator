package it.univr.reti;

import it.univr.reti.TCPSimulator.*;

public class Main {
	public static void main(String args[]) {
		double[][] rcvwnds = {				// [0]: timestamp, [1]: value
			{ 0, 12000 },
			{ 4, 24000 },
			{ 8, 9000 },
		};

		double[][] networkDowns = { 		// [0]: start, [1]: finish
			{ 8.5, 9.5 },
		};

		new TCPSimulator(
			1500, 					// mss
			96000, 				// data
			SSTRESHValue.INITIAL_RCVWND, 	// ssthresh: INITIAL_RCVWND or HALF_INITIAL_RCVWND allowed
			networkDowns, 					// network down(s)
			rcvwnds, 						// rcvwnds
			1,							// rtt
			RTOValue.DOUBLE_RTT 			// rto: DOUBLE_RTT allowed
		).simulate();
	}
}
