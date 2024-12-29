package it.univr.reti;

import it.univr.reti.Plot.AxisFormat;
import it.univr.reti.Plot.DataSeriesOptions;
import it.univr.reti.Plot.Line;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

class TCPPlot {

    // WINDOW & FILES PARAMETERS
    private static final int WIDTH = 900;
    private static final int HEIGHT = 650;
    private static final int PADDING = 27;
    private static final String TITLE = "TCP Plot";
    private static final Color TRANSPARENT_LIGHT_BLUE = new Color(33 / 255f, 170 / 255f, 1f, .4f);

    // PLOT LABELS
    private static final String X_AXIS_LABEL = "Time";
    private static final String Y_AXIS_LABEL = "Values";
    public static final String CWND_LABEL = "CWND";
    public static final String SSTHRESH_LABEL = "SSTHRESH";
    public static final String RCVWND_LABEL = "RCVWND";
    public static final String NETWORK_DOWN_LABEL = "Network Down";
    public static final String SEGMENTS_LOST_LABEL = "Segments Lost";
    public static final String CONNECTION_TIMED_OUT_LABEL = "Timeout";

    // INDEXES
    private static final int X_VALUES_INDEX = 0;
    private static final int Y_VALUES_INDEX = 1;

    // AUXILIARY VARIABLES
    private final Plot plot; // actual plot
    private final Map<Integer, double[]> networkDowns = new HashMap<>(); // in sec {start, finish}
    private final Map<String, List<double[]>> data = new HashMap<>(); // in segments {time, value}
    private static final Map<String, DataSeriesOptions> options = Map.ofEntries( // graphic options for each series
            Map.entry(CWND_LABEL,
                    Plot.seriesOpts().line(Line.NONE).color(Color.BLACK).marker(Plot.Marker.CIRCLE)
                            .markerColor(Color.BLACK)),
            Map.entry(SSTHRESH_LABEL, Plot.seriesOpts().color(Color.blue)),
            Map.entry(RCVWND_LABEL, Plot.seriesOpts().color(Color.GREEN)),
            Map.entry(NETWORK_DOWN_LABEL,
                    Plot.seriesOpts().color(Color.WHITE).areaColor(TRANSPARENT_LIGHT_BLUE)
                            .line(Line.DASHED)),
            Map.entry(SEGMENTS_LOST_LABEL,
                    Plot.seriesOpts().line(Line.NONE).color(Color.RED).marker(Plot.Marker.X)
                            .markerColor(Color.RED)),
            Map.entry(CONNECTION_TIMED_OUT_LABEL,
                    Plot.seriesOpts().line(Line.NONE).color(Color.RED).marker(Plot.Marker.DOUBLE_LINE)
                            .markerColor(Color.BLACK)));

    public TCPPlot() {
        plot = Plot.plot(Plot.plotOpts().title(TITLE)
                .legend(Plot.LegendFormat.BOTTOM)
                .width(WIDTH)
                .height(HEIGHT)
                .padding(PADDING));
    }

    public void addPointToPlot(String name, double xValue, double yValue) {
        data.computeIfAbsent(name, k -> new ArrayList<double[]>()).add(new double[]{xValue, yValue});
    }

    public void addNetworkDownToPlot(int count, double[] range) {
        networkDowns.put(count, range);
    }

    public void showPlot() {
        int maxY = -1, maxX = -1, gridDensity = 1;

        for (String name : data.keySet()) { // find max values for axes
            for (double[] value : data.get(name)) {
                if (value[Y_VALUES_INDEX] > maxY) {
                    maxY = (int) value[Y_VALUES_INDEX];
                }

                if (value[X_VALUES_INDEX] > maxX) {
                    maxX = (int) value[X_VALUES_INDEX];
                }
            }
        }

        maxY++; // add extra room

        List<double[]> temp; // check if rtt is decimal
        if ((temp = data.getOrDefault(CWND_LABEL, null)) != null) {
            if (temp.get(1)[X_VALUES_INDEX] % 1 != 0) {
                gridDensity = 2;
            }
        }

        plot.xAxis(X_AXIS_LABEL, Plot.axisOpts().range(0, maxX).format(AxisFormat.NUMBER));
        plot.yAxis(Y_AXIS_LABEL, Plot.axisOpts().range(0, maxY).format(AxisFormat.NUMBER_INT));
        plot.opts().grids(maxX * gridDensity, maxY);

        for (int i = 0; i < networkDowns.size(); i++) { // add network downs to plot
            double[] range = networkDowns.get(i);
            plot.series(NETWORK_DOWN_LABEL + " (" + (i + 1) + ")",
                    Plot.data().xy(range[X_VALUES_INDEX], 0)
                            .xy(range[X_VALUES_INDEX], maxY)
                            .xy(range[Y_VALUES_INDEX], maxY)
                            .xy(range[Y_VALUES_INDEX], 0),
                    options.get(NETWORK_DOWN_LABEL));
        }

        // needed to have the right z-index of the series
        List<String> names = new ArrayList<>(data.keySet());
        names.add(names.remove(0)); // used to bring cwnd & segments lost to the end
        names.add(names.remove(0));

        for (String name : names) { // add series to plot
            int size = data.get(name).size();
            double[] xValues = new double[size], yValues = new double[size], value;

            for (int i = 0; i < size; i++) { // extract values
                value = data.get(name).get(i);
                xValues[i] = value[X_VALUES_INDEX];
                yValues[i] = value[Y_VALUES_INDEX];
            }

            plot.series(name, Plot.data().xy(xValues, yValues), options.get(name));
        }

        JFrame f = new JFrame(TITLE); // show plot in a frame
        f.add(new JLabel(new ImageIcon(plot.getImage())));
        f.setResizable(false);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
    }
}