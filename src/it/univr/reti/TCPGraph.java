package it.univr.reti;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import it.univr.reti.Plot.DataSeriesOptions;
import it.univr.reti.Plot.Line;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;

class TCPGraph {
    // line graph to show status of:
    // - cwnd
    // - ssthresh
    // - rcvwnd
    // - network down

    private final Plot plot;
    private final Map<Integer, double[]> networkDowns = new HashMap<Integer, double[]>();
    private final Map<String, List<double[]>> data = new HashMap<String, List<double[]>>();
    private final Map<String, DataSeriesOptions> options = Map.ofEntries(
            Map.entry("cwnd",
                    Plot.seriesOpts().line(Line.NONE).color(Color.BLACK).marker(Plot.Marker.CIRCLE)
                            .markerColor(Color.BLACK)),
            Map.entry("ssthresh", Plot.seriesOpts().color(Color.RED)),
            Map.entry("rcvwnd", Plot.seriesOpts().color(Color.GREEN)),
            Map.entry("network down",
                    Plot.seriesOpts().color(Color.WHITE).areaColor(new Color(33 / 255f, 170 / 255f, 1f, .5f))
                            .line(Line.DASHED)));
    private final double rtt;

    public TCPGraph(double rtt) {
        plot = Plot.plot(Plot.plotOpts().title("TCP Graph").legend(Plot.LegendFormat.BOTTOM).width(1000).height(700))
                .xAxis("Time", null).yAxis("Window Size", null);
        this.rtt = rtt;
    }

    public void addPointToPlot(String name, double xValue, double yValue) {
        if (data.containsKey(name)) {
            data.get(name).add(new double[] { xValue, yValue });
        } else {
            List<double[]> values = new ArrayList<double[]>();
            values.add(new double[] { xValue, yValue });
            data.put(name, values);
        }
    }

    public void addNetworkDownToPlot(int count, double[] range) {
        networkDowns.put(count, range);
    }

    public void showGraph() {
        try {
            int maxY = 0, maxX = 0;
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

            plot.xAxis("Time", Plot.axisOpts().range(0, maxX));
            plot.yAxis("Values", Plot.axisOpts().range(0, maxY));
            plot.opts().grids(maxX, maxY);

            for (int i = 0; i < networkDowns.size(); i++) {
                double[] range = networkDowns.get(i);
                plot.series("network down " + (i + 1),
                        Plot.data().xy(range[0], 0).xy(range[0], maxY).xy(range[1], maxY)
                                .xy(range[1], 0).xy(range[0], 0),
                        options.get("network down"));
            }

            for (String name : data.keySet()) {
                int size = data.get(name).size();

                double[] xValues = new double[size];
                double[] yValues = new double[size];

                for (int i = 0; i < size; i++) {
                    double[] value = data.get(name).get(i);
                    xValues[i] = value[0] * rtt;
                    yValues[i] = value[1];
                }

                plot.series(name, Plot.data().xy(xValues, yValues), options.get(name));
            }

            plot.save("plot", "png");

            BufferedImage img = ImageIO.read(new File("plot.png"));
            ImageIcon icon = new ImageIcon(img);
            JFrame frame = new JFrame();
            frame.setLayout(new FlowLayout());
            frame.setSize(plot.opts().width(), plot.opts().height());
            JLabel lbl = new JLabel();
            lbl.setIcon(icon);
            frame.add(lbl);
            frame.setTitle("TCP Graph");
            frame.setVisible(true);

            // delete picture on close
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            File file = new File("plot.png");
            file.delete();
        } catch (IOException e) {
            System.out.println("Error while saving graph!");
        }
    }
}