package it.univr.reti;

public class Main {
	public static void main(String args[]) {
		double[][] rcvwnds = { // [0]: timestamp, [1]: value
				{ 0, 19200 },
				{ 5, 24000 },
				{ 11, 19200 }
		};

		double[][] networkDowns = { // [0]: start, [1]: finish
				{ 5.5, 8 },
				{ 18.5, 19.5 }
		};

		new TCPSimulator(
				1200, // mss
				73200, // data
				TCPSimulator.INITIAL_RCVWND, // ssthresh: INITIAL_RCVWND or HALF_INITIAL_RCVWND allowed
				networkDowns, // network down(s)
				rcvwnds, // rcvwnds
				1, // rtt
				TCPSimulator.DOUBLE_RTT // rto: DOUBLE_RTT allowed
		).simulate();
	}
}
