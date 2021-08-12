package com.melahn.util.helm;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.melahn.util.helm.model.HelmChart;
import com.melahn.util.helm.model.HelmMaintainer;

public class ChartMapPrinter implements IChartMapPrinter {

    protected HelmChart chart;
    ChartKeyMap charts;
    protected ChartMap chartMap;
    protected String outputFilename;
    protected int indent = 2; // indent for tree view
    protected FileWriter writer;
    protected static final String NOT_SPECIFIED = "Not specified";

    ChartMapPrinter(ChartMap chartMap, String outputFilename, ChartKeyMap charts, HelmChart chart)
            throws ChartMapException {
        this.outputFilename = outputFilename;
        this.chart = chart;
        this.charts = charts;
        this.chartMap = chartMap;
        try {
            writer = new FileWriter(outputFilename);
        } catch (IOException e) {
            Logger logger = chartMap.logger == null?LogManager.getLogger():chartMap.logger;
            logger.error("{}{}{}{}", "Error creating FileWriter for file ", outputFilename, " : ",
                    e.getMessage());
            throw new ChartMapException(e.getMessage());
        }
    }

    protected void writeLine(String l) throws ChartMapException {
        try {
            writer.write(l + "\n");
            writer.flush();
        } catch (IOException e) {
            Logger logger = chartMap.logger == null?LogManager.getLogger():chartMap.logger;
            logger.error("{}{}", "Error writing line to file ", outputFilename);
            throw new ChartMapException(e.getMessage());
        }
    }

    protected String formatString(String s) {
        if (s == null || s.trim().isEmpty()) {
            return NOT_SPECIFIED;
        } else {
            return s;
        }
    }

    public void printHeader() throws ChartMapException  {
        writeLine("Chart Map for " + chart.getNameFull());
    }

    public void printFooter() throws ChartMapException  {
        writeLine("");
        writeLine("Generated on " + getCurrentDateTime() + " by " + this.getClass().getCanonicalName() + " ("
                + getGitHubRepoURL() + ")");
    }

    public String getGitHubRepoURL() {
        return "https://github.com/melahn/helm-chartmap";
    }

    public void printChartToChartDependency(HelmChart parentChart, HelmChart dependentChart)  throws ChartMapException {
        writeLine(parentChart.getNameFull() + " depends on " + dependentChart.getNameFull());
    }

    public void printChartToImageDependency(HelmChart chart, String imageName) throws ChartMapException {
        writeLine(chart.getNameFull() + " uses " + imageName);
    }

    public void printChart(HelmChart chart) throws ChartMapException {
        writeLine("Chart: " + chart.getNameFull());
        writeLine("\tapiVersion: " + formatString(chart.getApiVersion()));
        writeLine("\tappVersion: " + formatString(chart.getAppVersion()));
        writeLine("\tcreated: " + formatString(chart.getCreated()));
        writeLine("\tdependencies: " + formatDependencies(chart.getDiscoveredDependencies()));
        writeLine("\tdescription: " + formatString(chart.getDescription()));
        writeLine("\tdigest: " + formatString(chart.getDigest()));
        writeLine("\ticon: " + formatString(chart.getIcon()));
        writeLine("\tkeywords: " + formatArray(chart.getKeywords()));
        writeLine("\tmaintainers: " + formatMaintainers(chart.getMaintainers()));
        if (chart.getRepoUrl() != null) {
            writeLine("\trepo url: " + chart.getRepoUrl());
        }
        writeLine("\tname: " + formatString(chart.getName()));
        writeLine("\tsources: " + formatArray(chart.getSources()));
        writeLine("\turls: " + formatArray(chart.getUrls()));
        writeLine("\tversion: " + formatString(chart.getVersion()));
    }

    public void printImage(String c) throws ChartMapException {
        writeLine("Image: " + c);
    }

    public void printComment(String comment) throws ChartMapException {
        writeLine(comment);
    }

    public void printSectionHeader(String header) throws ChartMapException {
        StringBuilder sb = new StringBuilder();
        for (int c = 1; c <= header.length(); c++) {
            sb.append('-');
        }
        writeLine("");
        writeLine(header);
        writeLine(sb.toString());
    }

    public void setOutputFilename(String o) {
        this.outputFilename = o;
    }

    public String getOutputFilename() {
        return outputFilename;
    }

    public void setIndent(int i) {
        indent = i;
    }

    public int getIndent() {
        return indent;
    }

    public void setChart(HelmChart chart) {
        this.chart = chart;
    }

    public HelmChart getChart() {
        return chart;
    }

    /**
     * Helm V3 introduced the library chart type so return the value of type if
     * found.
     *
     * @param c a helm chart
     * @return the string "library" if this is a library chart, "application"
     *         otherwise
     */
    public String getChartType(HelmChart c) {
        if (c.getType() != null && c.getType().equals("library")) {
            return c.getType();
        } else {
            return "application";
        }
    }

    protected String getCurrentDateTime() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        return (f.format(LocalDateTime.now()));
    }

    /**
     * Returns a string with the elements of an array separated by commas
     *
     * @param a the array to be formatted
     * @return the string form of the array
     */
    private String formatArray(String[] a) {
        StringBuilder sb = new StringBuilder("");
        if (a != null) {
            for (int i = 0; i < a.length; i++) {
                sb.append(a[i]);
                if (i != a.length - 1) {
                    sb.append(",");
                }
            }
        } else {
            sb = sb.append(NOT_SPECIFIED);
        }
        return sb.toString();
    }

    /**
     * Returns a string with the name and email addresses of the maintainers of a
     * Helm Chart separated by commas
     *
     * @param m an array of Helm Maintainers
     * @return the string form of the maintainers array
     */
    private String formatMaintainers(HelmMaintainer[] m) {
        StringBuilder sb = new StringBuilder("");
        if (m != null) {
            for (int i = 0; i < m.length; i++) {
                sb.append(m[i].getName());
                String email = m[i].getEmail();
                if (email != null) {
                    sb.append(":");
                    sb.append(email);
                }
                if (i != m.length - 1) {
                    sb.append(",");
                }
            }
        } else {
            sb = sb.append((NOT_SPECIFIED));
        }
        return sb.toString();
    }

    private String formatDependencies(Set<HelmChart> d) {
        StringBuilder sb = new StringBuilder("");
        if (d.isEmpty()) {
            sb.append("None");
        } else {
            boolean first = true;
            Iterator<HelmChart> i = d.iterator();
            while (i.hasNext()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(i.next().getNameFull());
                first = false;
            }
            return sb.toString();
        }
        return sb.toString();
    }

    public void printTree(HelmChart c) throws ChartMapException {
        // see issue #9
    }

}
