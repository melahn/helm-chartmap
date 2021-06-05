package com.melahn.util.helm;

import com.melahn.util.helm.model.HelmChart;
import com.melahn.util.helm.model.HelmMaintainer;

import java.io.IOException;

/**
 * A class that generates a PlantUML file describing a Kubernetes Helm Chart and
 * its dependencies.
 *
 */
public class PlantUmlChartMapPrinter extends ChartMapPrinter {

    private String[] colors;
    private static final String SEPARATOR = "\\n====\\n";

    /**
     * Constructor
     *
     * @param chartMap       the instance of chartMap using this printer
     * @param outputFilename the name of the PlantUML file to be created.
     * @param charts         a multi-key map of all the Helm Charts that might be
     *                       referenced. The map is keyed by Chart Name and Chart
     *                       Version.
     * @param chart          a Helm Chart to be printed in PlantUML format
     */
    public PlantUmlChartMapPrinter(ChartMap chartMap, String outputFilename, ChartKeyMap charts, HelmChart chart)
            throws ChartMapException {
        super(chartMap, outputFilename, charts, chart);
        initializeColors();
    }

    /**
     * Writes the start of a PlantUML diagram including a title
     *
     * @throws IOException IOException
     */
    @Override
    public void printHeader() throws ChartMapException {
        writeLine("@startuml");
        writeLine("skinparam linetype ortho");
        writeLine("skinparam backgroundColor white");
        writeLine("skinparam usecaseBorderColor black");
        writeLine("skinparam usecaseArrowColor LightSlateGray");
        writeLine("skinparam artifactBorderColor black");
        writeLine("skinparam artifactArrowColor LightSlateGray");
        writeLine("");
        writeLine("title Chart Map for " + chart.getNameFull());
    }

    /**
     * Writes a PlantUML footer with a time stamp and a reference to this class and
     * GitHub project
     *
     * @throws IOException IOException
     */
    @Override
    public void printFooter() throws ChartMapException {
        writeLine("");
        writeLine("center footer Generated on " + getCurrentDateTime() + " by " + this.getClass().getCanonicalName()
                + "\\n" + getGitHubRepoURL());
        writeLine("@enduml");
    }

    /**
     * Writes a line that shows the dependency of one Helm Chart on another
     *
     * @param parentChart    the parent Helm Chart
     * @param dependentChart a Helm Chart on which the parent Helm Chart depends
     * @throws IOException IOException
     */
    @Override
    public void printChartToChartDependency(HelmChart parentChart, HelmChart dependentChart) throws ChartMapException {
        writeLine(getNameAsPlantUmlReference(parentChart.getNameFull()) + "--[#green]-|>"
                + getNameAsPlantUmlReference(dependentChart.getNameFull()));
    }

    /**
     * Writes a line that shows the dependency of a Chart on an Image
     *
     * @param chart     a Helm Chart
     * @param imageName the name of a Docker Image on which the Helm Chart depends
     * @throws IOException IOException
     */
    @Override
    public void printChartToImageDependency(HelmChart chart, String imageName) throws ChartMapException {
        writeLine(getNameAsPlantUmlReference(chart.getNameFull()) + "--[#orange]-|>"
                + getNameAsPlantUmlReference(imageName));
    }

    /**
     * Writes a line to depict a Helm Chart
     *
     * @param chart a Helm Chart
     * @throws IOException IOException
     */
    @Override
    public void printChart(HelmChart chart) throws ChartMapException {
        writeLine("artifact \"" + chart.getNameFull() + getComponentBody(chart) + "\" as "
                + getNameAsPlantUmlReference(chart.getNameFull()) + " " + getChartArtifactColor(chart));
    }

    /**
     * Writes a line to depict a Docker Image
     */
    @Override
    public void printImage(String imageName) throws ChartMapException {
        // e.g. image: "quay.io/alfresco/service-sync:2.2-SNAPSHOT"
        writeLine("usecase \"" + getImageBody(imageName) + "\" as " + getNameAsPlantUmlReference(imageName) + " "
                + getImageArtifactColor(imageName));
    }

    /**
     * Writes a comment line
     *
     * @param comment the comment to be written
     * @throws IOException IOException
     */
    @Override
    public void printComment(String comment) throws ChartMapException {
        writeLine("'" + comment);
    }

    /**
     * Writes a section header
     *
     * @param header the header to be written
     * @throws IOException IOException
     */
    @Override
    public void printSectionHeader(String header) throws ChartMapException {
        try {
            writeLine("");
            writer.write("'" + header + "\n");
        } catch (IOException e) {
            throw new ChartMapException(e.getMessage());
        }
    }

    /**
     * Returns the text to use in a PlantUML artifact that describes a Helm Chart
     *
     * @param chart a Helm Chart
     * @return text that can be used for the body of a PlantUML artifact
     */
    private String getComponentBody(HelmChart chart) {
        String body = getSeparator();
        body += "\\t" + chart.getName();
        body += getSeparator();
        body += "\\t" + chart.getVersion();
        body += getSeparator();
        body += getChartType(chart);
        body += getSeparator();
        if (chart.getRepoUrl() != null) {
            body += "\\t" + chart.getRepoUrl();
        }
        body += getSeparator();
        body += "\\t" + getMaintainers(chart.getMaintainers());
        body += getSeparator();
        body += "\\t" + getKeywords(chart.getKeywords());
        return body;
    }

    /**
     * Returns the text to use in a PlantUML artifact that describes a Docker Image
     *
     * @param i the name of a Docker Image
     * @return text that can be used for the body of a PlantUML artifact
     */
    private String getImageBody(String i) {
        String imageName = null;
        String body = "Image";
        body += getSeparator();
        String repoHost = "Docker Hub";
        String s = i;
        int count = s.length() - s.replace("/", "").length();
        if (count == 0) { // e.g. postgres:9.6.2
            imageName = i.substring(0, i.indexOf(':'));
        } else if (count == 1) { // e.g. : alfresco/process-services:1.8.0
            imageName = i.substring(0, i.indexOf(':'));
        } else { // e.g. quay.io/alfresco/service:1.0.0
            repoHost = i.substring(0, i.indexOf('/'));
            imageName = i.substring(i.indexOf('/') + 1, i.length());
        }
        body += "\\t" + repoHost;
        body += getSeparator();
        body += "\\t" + imageName;
        body += getSeparator();
        String version = "?";
        if (i.contains(":")) {
            version = i.substring(i.indexOf(':') + 1, i.length());
        }
        body += "\\t" + version;
        return body;
    }

    /**
     * Returns a PlantUML separator
     *
     * @return PlantUML text for a separator
     */
    private String getSeparator() {
        return SEPARATOR;
    }

    /**
     * Get the maintainers for a Helm Chart, nicely formatted
     *
     * @param m an array of maintainers discovered in a Helm Chart
     * @return a formatted String of the maintainers separated by commas
     */
    private String getMaintainers(HelmMaintainer[] m) {
        StringBuilder maintainers = new StringBuilder("Maintainers: ");
        boolean first = true;
        if (m != null) {
            for (HelmMaintainer hm : m) {
                if (first) {
                    maintainers.append(hm.getName());
                    first = false;
                } else {
                    maintainers.append(", " + hm.getName());
                }
            }
        }
        return maintainers.toString();
    }

    /**
     * Get the keywords for a Helm Chart, nicely formatted
     *
     * @param k an array of keywords discovered from a Helm Chart
     * @return a formatted String of the keywords one per line
     */
    private String getKeywords(String[] k) {
        StringBuilder keywords = new StringBuilder("Keywords: ");
        boolean first = true;
        if (k != null) {
            for (String aKeyword : k) {
                if (first) {
                    keywords.append(aKeyword);
                    first = false;
                } else {
                    keywords.append("\\n").append(aKeyword);
                }
            }
        }
        return keywords.toString();
    }

    /**
     *
     * Get the name of a Helm artifact as a name that obeys PlantUML rules
     *
     * @param s the full name of the the artifact
     * @return the name of the artifact where the characters that would violate
     *         PlantUML naming rules are replaced with underscores
     */
    private String getNameAsPlantUmlReference(String s) {
        String reference = s.replace(':', '_');
        reference = reference.replace('.', '_');
        reference = reference.replace('-', '_');
        reference = reference.replace('/', '_');
        return reference;
    }

    /**
     *
     * @param h a HelmChart for which you want a background color
     * @return a PlantUML color attribute chosen from the colors table
     */

    private String getChartArtifactColor(HelmChart h) {
        int hashValue = hashHelmChartName(h);
        return "#" + colors[hashValue];
    }

    /**
     *
     * @param imageName the name of a Docker Image for which you want a background
     *                  color
     * @return a PlantUML color attribute chosen from the colors table
     */
    private String getImageArtifactColor(String imageName) {
        int hashValue = hashImageName(imageName);
        return "#" + colors[hashValue];
    }

    /**
     *
     * @param h a HelmChart from which a hash code will be generated (for the
     *          purpose of indexing into the colors array). Only the name of the
     *          Helm chart is used to create the hash because the main point of
     *          choosing a color is to associate visually a group of related Helm
     *          Charts in the diagram (e.g. all the Postgresql Charts may be colored
     *          as 'Coral').
     *
     * @return a calculated hash value aa an unsigned int
     */
    private int hashHelmChartName(HelmChart h) {
        int hashCode = (h.getName().hashCode() * Integer.MAX_VALUE) / (Integer.MAX_VALUE / (getColors().length) * 2);
        hashCode = Math.abs(hashCode);
        return hashCode;
    }

    /**
     *
     * @param i the name of a Docker Image from which a hash code will be generated
     *          (for the purpose of indexing into the colors array). Only the name
     *          of the Image is used to create the hash because the main point of
     *          choosing a color is to associate visually a group of related Images
     *          in the diagram (e.g. all the Postgresql Images may be colored as
     *          'Chocolate').
     *
     * @return a calculated hash value as an unsigned int
     */
    private int hashImageName(String i) {
        String[] s = i.split(":");
        String baseName = s[0];
        int hashCode = (baseName.hashCode() * Integer.MAX_VALUE) / (Integer.MAX_VALUE / (getColors().length) * 2);
        hashCode = Math.abs(hashCode);
        return hashCode;
    }

    /**
     * Get the colors array
     * 
     * @return the colors array
     */

    private String[] getColors() {
        return colors;
    }

    /**
     * Initializes the colors array with values that PlantUML can use to decorate
     * the generated diagram.
     *
     * The color values are derived from http://plantuml.com/color
     *
     * Color values that are too dark to use with black text are commented out so
     * they are ineligible.
     *
     * PlantUML does support using other than black text so if you decide to use a
     * different color (using skinparam) you may decide to use these darker colors.
     * A matter of taste.
     *
     * But note that your choice of text color applies to the whole file.
     */

    private void initializeColors() {
        colors = new String[] { "AliceBlue", "AntiqueWhite", "Aqua", "Aquamarine", "Azure", "Beige", "Bisque",
                // "Black",
                "BlanchedAlmond",
                // "Blue",
                // "BlueViolet",
                // "Brown",
                "BurlyWood", "CadetBlue", "Chartreuse", "Chocolate", "Coral", "CornflowerBlue", "Cornsilk",
                // "Crimson",
                "Cyan",
                // "DarkBlue",
                // "DarkCyan",
                "DarkGoldenRod",
                // "DarkGray",
                // "DarkGreen",
                // "DarkGrey",
                // "DarkKhaki",
                // "DarkMagenta",
                // "DarkOliveGreen",
                // "DarkOrchid",
                // "DarkRed",
                "DarkSalmon", "DarkSeaGreen",
                // "DarkSlateBlue",
                // "DarkSlateGray",
                // "DarkSlateGrey",
                "DarkTurquoise",
                // "DarkViolet",
                "Darkorange",
                // "DeepPink",
                "DeepSkyBlue",
                // "DimGray",
                // "DimGrey",
                "DodgerBlue",
                // "FireBrick",
                "FloralWhite",
                // "ForestGreen",
                "Fuchsia", "Gainsboro", "GhostWhite", "Gold", "GoldenRod",
                // "Gray",
                // "Green",
                "GreenYellow",
                // "Grey",
                "HoneyDew", "HotPink", "IndianRed",
                // "Indigo",
                "Ivory", "Khaki", "Lavender", "LavenderBlush", "LawnGreen", "LemonChiffon", "LightBlue", "LightCoral",
                "LightCyan", "LightGoldenRodYellow", "LightGray", "LightGreen", "LightGrey", "LightPink", "LightSalmon",
                "LightSeaGreen", "LightSkyBlue",
                // "LightSlateGray",
                // "LightSlateGrey",
                "LightSteelBlue", "LightYellow", "Lime", "LimeGreen", "Linen",
                // "Magenta",
                "Maroon", "MediumAquaMarine",
                // "MediumBlue",
                "MediumOrchid", "MediumPurple", "MediumPurple", "MediumSeaGreen",
                // "MediumSlateBlue",
                "MediumSpringGreen", "MediumTurquoise",
                // "MediumVioletRed",
                // "MidnightBlue",
                "MintCream", "MistyRose", "Moccasin", "NavajoWhite",
                // "Navy",
                "OldLace", "Olive", "OliveDrab", "Orange", "OrangeRed",
                // "Orchid",
                "PaleGoldenRod", "PaleGreen", "PaleTurquoise", "PaleVioletRed", "PapayaWhip", "PeachPuff", "Peru",
                "Pink", "Plum", "PowderBlue",
                // "Purple",
                "Red", "RosyBrown", "RoyalBlue",
                // "SaddleBrown",
                "Salmon", "SandyBrown",
                // "SeaGreen",
                "SeaShell",
                // "Sienna",
                "Silver", "SkyBlue",
                // "SlateBlue",
                // "SlateGray",
                // "SlateGrey",
                "Snow", "SpringGreen",
                // "SteelBlue",
                "Tan",
                // "Teal",
                "Thistle", "Tomato", "Turquoise", "Violet", "Wheat", "White", "WhiteSmoke", "Yellow", "YellowGreen" };
    }
}
