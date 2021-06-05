package com.melahn.util.helm;

import com.melahn.util.helm.model.HelmChart;

public interface IChartMapPrinter {

    void printComment(String s) throws ChartMapException;

    void printHeader() throws ChartMapException ;

    void printFooter() throws ChartMapException ;

    void printChartToChartDependency(HelmChart parentChart, HelmChart dependentChart) throws ChartMapException;

    void printChartToImageDependency(HelmChart parentChart, String imageName) throws ChartMapException;

    void printChart(HelmChart chart)  throws ChartMapException;

    void printImage(String s) throws ChartMapException;

    void printTree(HelmChart chart) throws ChartMapException;

    void printSectionHeader(String header) throws ChartMapException;

    void setOutputFilename (String outputFilename);

    String getOutputFilename ();

    String getGitHubRepoURL();

    void setChart(HelmChart chart);

    HelmChart getChart ();
}
