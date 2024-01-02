package it.univr.reti;

public class Main {
	public static void main(String args[]) {
		double[][] rcvwnds = { // [0]: timestamp, [1]: value
				{ 0, 19600 },
				{ 13, 5600 },
				{ 15, 14000 }
		};

		double[][] networkDowns = { // [0]: start, [1]: finish
				{ 7.5, 8.5 },
				{ 18, 20 }
		};

		new TCPSimulator(
				1400, // mss
				145600, // data
				TCPSimulator.INITIAL_RCVWND, // ssthresh: INITIAL_RCVWND or HALF_INITIAL_RCVWND allowed
				networkDowns, // network down(s)
				rcvwnds, // rcvwnds
				1, // rtt
				TCPSimulator.DOUBLE_RTT // rto: DOUBLE_RTT allowed
		).simulate();
	}
}
