package it.univr.reti;

public class Main {
	public static void main(String args[]) {
		double[][] rcvwnds = { // [0]: timestamp, [1]: value
				{ 0, 12800 },
				{ 6, 6400 },
				{ 8, 12800 },
		};

		double[][] networkDowns = { // [0]: start, [1]: finish
				{ 6.5, 7.5 },
				{ 14.5, 15.5 }
		};

		new TCPSimulator(
				800, // mss
				67200, // data
				TCPSimulator.INITIAL_RCVWND, // ssthresh: INITIAL_RCVWND or HALF_INITIAL_RCVWND allowed
				networkDowns, // network down(s)
				rcvwnds, // rcvwnds
				1, // rtt
				TCPSimulator.DOUBLE_RTT // rto: DOUBLE_RTT allowed
		).simulate();
	}
}
