package it.univr.reti;

public class Main {
	public static void main(String args[]) {
		double[][] rcvwnds = { // [0]: timestamp, [1]: value
				{ 0, 19200 },
		};

		double[][] networkDowns = { // [0]: start, [1]: finish
				{ 3, 3.5 },
				{ 5.5, 6 }
		};

		new TCPSimulator(
				1200, // mss
				72000, // data
				TCPSimulator.INITIAL_RCVWND, // ssthresh: INITIAL_RCVWND or HALF_INITIAL_RCVWND allowed
				networkDowns, // network down(s)
				rcvwnds, // rcvwnds
				0.5, // rtt
				TCPSimulator.DOUBLE_RTT // rto: DOUBLE_RTT allowed
		).simulate();
	}
}
