package com.melahn.util.helm;

import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Iterator;

import com.melahn.util.helm.model.HelmMaintainer;
import org.apache.commons.collections4.map.MultiKeyMap;

import com.melahn.util.helm.model.HelmChart;

public class ChartMapPrinter implements IChartMapPrinter {

    protected HelmChart chart;
    protected String outputFilename;
    protected int indent=2; // indent for tree view
    protected FileWriter writer;

    ChartMapPrinter(String outputFilename, MultiKeyMap charts, HelmChart chart) {
        this.outputFilename = outputFilename;
        this.chart = chart;
        try {
            writer = new FileWriter(outputFilename);
        } catch (IOException e) {
            System.out.println("Error creating FileWriter for file " + outputFilename + " : " + e.getMessage());
        }
    }

    void writeLine(String l) throws IOException {
        try {
            writer.write(l + "\n");
            writer.flush();
        } catch (IOException e) {
            System.out.println("Error writing line to file " + outputFilename);
            throw (e);
        }
    }

    String formatString(String v) {
        if (v == null || v.trim().isEmpty() ) {
            return "Not specified";
        }
        else {
            return v;
        }
    }

    public void printHeader() throws IOException {
        writeLine("Chart Map for " + chart.getNameFull());
    }

    public void printFooter() throws IOException {
        writeLine("");
        writeLine("Generated on " + getCurrentDateTime() + " by " + this.getClass().getCanonicalName() + " (" + getGitHubRepoURL() + ")");
    }

    public String getGitHubRepoURL() {
        return "https://github.com/melahn/helm-chartmap";
    }

    public void printChartToChartDependency(HelmChart parentChart, HelmChart dependentChart) throws IOException {
        writeLine(parentChart.getNameFull() + " depends on " + dependentChart.getNameFull());
    }

    public void printChartToImageDependency(HelmChart chart, String imageName) throws IOException {
        writeLine(chart.getNameFull() + " uses " + imageName);
    }

    public void printChart(HelmChart chart) throws IOException {
        writeLine("Chart: " + chart.getNameFull());
        writeLine("\tapiVersion: " + formatString(chart.getApiVersion()));
        writeLine("\tappVersion: " + formatString(chart.getAppVersion()));
        writeLine("\tcreated: " + formatString(chart.getCreated()));
        writeLine("\tdependencies: " + formatDependencies(chart.getDependencies()));
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

    public void printImage(String c) throws IOException {
        writeLine("Image: " + c);
    }

    public void printComment(String comment) throws IOException {
        writeLine(comment);
    }

    public void printSectionHeader(String header) throws IOException {
        StringBuffer sb = new StringBuffer();
        for(int c = 1; c <= header.length(); c++){
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

    String getCurrentDateTime() {
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
        if (a !=null) {
            for (int i = 0; i < a.length; i++) {
                sb.append(a[i]);
                if (i != a.length - 1) {
                    sb.append(",");
                }
            }
        }
        else {
            sb = sb.append("Not specified");
        }
        return sb.toString();
    }

    /**
     * Returns a string with the name and email addresses of the maintainers of a Helm Chart
     * separated by commas
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
        }
        else {
            sb = sb.append(("Not specified"));
        }
        return sb.toString();
    }

    private String formatDependencies(HashSet<HelmChart> d) {
        StringBuilder sb = new StringBuilder("");
        if (d.size() == 0) {
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

    public void printTree(HelmChart c) throws IOException {
        // TODO add a generic printTree method
    }

}
