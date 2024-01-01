package it.univr.reti;

public class Main {
	public static void main(String args[]) {
		double[][] rcvwnds = { // [0]: timestamp, [1]: value
				{ 0, 17600 },
				{ 2.5, 5500 },
				{ 3.5, 22000 },
		};

		double[][] networkDowns = { // [0]: start, [1]: finish
				{ 4.5, 5.5 },
		};

		new TCPSimulator(
				1100, // mss
				85800, // data
				TCPSimulator.INITIAL_RCVWND, // ssthresh: INITIAL_RCVWND or HALF_INITIAL_RCVWND allowed
				networkDowns, // network down(s)
				rcvwnds, // rcvwnds
				0.5, // rtt
				TCPSimulator.DOUBLE_RTT // rto: DOUBLE_RTT allowed
		).simulate();
	}
}
