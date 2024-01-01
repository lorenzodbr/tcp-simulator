package it.univr.reti;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import it.univr.reti.Plot.AxisFormat;
import it.univr.reti.Plot.DataSeriesOptions;
import it.univr.reti.Plot.Line;

import java.awt.Color;

class TCPGraph {
    // WINDOW & FILES PARAMETERS
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 700;
    private static final String ERROR_MESSAGE = "An error occurred while creating the graph.";
    private static final String ERROR_TITLE = "Error";
    private static final String FILE_NAME = "graph";
    private static final String FILE_EXTENSION = "png";
    private static final String FILE_PATH = FILE_NAME + "." + FILE_EXTENSION;
    private static final String TITLE = "TCP Graph";

    // GRAPH LABELS
    private static final String X_AXIS_LABEL = "Time";
    private static final String Y_AXIS_LABEL = "Values";
    public static final String CWND_LABEL = "CWND";
    public static final String SSTHRESH_LABEL = "SSTHRESH";
    public static final String RCVWND_LABEL = "RCVWND";
    public static final String NETWORK_DOWN_LABEL = "Network Down";
    public static final String SEGMENTS_LOST_LABEL = "Segments Lost";

    private final Plot plot;
    private final Map<Integer, double[]> networkDowns = new HashMap<Integer, double[]>();
    private final Map<String, List<double[]>> data = new HashMap<String, List<double[]>>();
    private final Map<String, DataSeriesOptions> options = Map.ofEntries(
            Map.entry(CWND_LABEL,
                    Plot.seriesOpts().line(Line.NONE).color(Color.BLACK).marker(Plot.Marker.CIRCLE)
                            .markerColor(Color.BLACK)),
            Map.entry(SSTHRESH_LABEL, Plot.seriesOpts().color(Color.RED)),
            Map.entry(RCVWND_LABEL, Plot.seriesOpts().color(Color.GREEN)),
            Map.entry(NETWORK_DOWN_LABEL,
                    Plot.seriesOpts().color(Color.WHITE).areaColor(new Color(33 / 255f, 170 / 255f, 1f, .5f))
                            .line(Line.DASHED)),
            Map.entry(SEGMENTS_LOST_LABEL,
                    Plot.seriesOpts().line(Line.NONE).color(Color.RED).marker(Plot.Marker.DIAMOND)
                            .markerColor(Color.RED)));

    public TCPGraph() {
        plot = Plot.plot(Plot.plotOpts().title(TITLE).legend(Plot.LegendFormat.BOTTOM).width(WIDTH).height(HEIGHT));
    }

    public void addPointToPlot(String name, double xValue, double yValue) {
        data.computeIfAbsent(name, k -> new ArrayList<double[]>()).add(new double[] { xValue, yValue });
    }

    public void addNetworkDownToPlot(int count, double[] range) {
        networkDowns.put(count, range);
    }

    public void showGraph() {
        try {
            int maxY = -1, maxX = -1;
            boolean rttZeroPointFive = false;

            for (String name : data.keySet()) {
                for (double[] value : data.get(name)) {
                    if (value[1] > maxY) {
                        maxY = (int) value[1];
                    }

                    if (value[0] > maxX) {
                        maxX = (int) value[0];
                    }
                }
            }

            maxY++;

            List<double[]> temp;
            if ((temp = data.getOrDefault(CWND_LABEL, null)) != null) {
                if (temp.get(1)[0] % 1 != 0) {
                    rttZeroPointFive = true;
                }
            }

            plot.xAxis(X_AXIS_LABEL, Plot.axisOpts().range(0, maxX).format(rttZeroPointFive ? AxisFormat.NUMBER
                    : AxisFormat.NUMBER_INT));
            plot.yAxis(Y_AXIS_LABEL, Plot.axisOpts().range(0, maxY).format(AxisFormat.NUMBER_INT));

            plot.opts().grids(maxX * (rttZeroPointFive ? 2 : 1), maxY);

            for (int i = 0; i < networkDowns.size(); i++) {
                double[] range = networkDowns.get(i);
                plot.series(NETWORK_DOWN_LABEL + " (" + (i + 1) + ")",
                        Plot.data().xy(range[0], 0).xy(range[0], maxY).xy(range[1], maxY)
                                .xy(range[1], 0).xy(range[0], 0),
                        options.get(NETWORK_DOWN_LABEL));
            }

            for (String name : data.keySet()) {
                int size = data.get(name).size();

                double[] xValues = new double[size];
                double[] yValues = new double[size];

                for (int i = 0; i < size; i++) {
                    double[] value = data.get(name).get(i);
                    xValues[i] = value[0];
                    yValues[i] = value[1];
                }

                plot.series(name, Plot.data().xy(xValues, yValues), options.get(name));
            }

            plot.save(FILE_NAME, FILE_EXTENSION);

            JFrame f = new JFrame(TITLE);
            ImageIcon icon = new ImageIcon(FILE_PATH);
            f.add(new JLabel(icon));
            f.setResizable(false);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            new File(FILE_PATH).delete();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, ERROR_MESSAGE, ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
        }
    }
}