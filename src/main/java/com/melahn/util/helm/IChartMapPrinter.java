package com.melahn.util.helm;

import com.melahn.util.helm.model.HelmChart;

import java.io.IOException;

public interface IChartMapPrinter {

    void printComment(String s) throws IOException;

    void printHeader() throws IOException;

    void printFooter() throws IOException;

    void printChartToChartDependency(HelmChart parentChart, HelmChart dependentChart) throws IOException;

    void printChartToImageDependency(HelmChart parentChart, String imageName) throws IOException;

    void printChart(HelmChart chart)  throws IOException;

    void printImage(String s) throws IOException;

    void printSectionHeader(String header) throws IOException;

    void setOutputFilename (String outputFilename);

    String getOutputFilename ();

    String getGitHubRepoURL();

    void setChart(HelmChart chart);

    HelmChart getChart ();
}
