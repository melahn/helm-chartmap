package org.alfresco.deployment.util;

import org.alfresco.deployment.util.model.HelmDeploymentContainer;
import org.apache.commons.collections4.map.MultiKeyMap;

import org.alfresco.deployment.util.model.HelmChart;
import org.alfresco.deployment.util.model.HelmMaintainer;

import java.io.IOException;

/**
 * A class that generates a PlantUML file describing a Kubernetes
 * Helm Chart and its dependencies.
 *
 */
public class PlantUmlChartMapPrinter extends ChartMapPrinter {

    private String[] colors;

    /**
     * Constructor
     *
     * @param   outputFilename  the name of the PlantUML file to be created.
     * @param   charts          a multi-key map of all the Helm Charts that
     *                          might be referenced.  The map is keyed by
     *                          Chart Name and Chart Version.
     * @param   chart           a Helm Chart to be printed in PlantUML format
     */
    public PlantUmlChartMapPrinter(String outputFilename, MultiKeyMap charts, HelmChart chart) {
        super(outputFilename, charts, chart);
        initializeColors();
    }

    /**
     * Writes the start of a PlantUML diagram including a title
     *
     * @throws IOException      IOException
     */
    public void printHeader() throws IOException {
        writeLine("@startuml");
        writeLine("skinparam linetype ortho");  // TODO: get these from config
        writeLine("skinparam backgroundColor white");
        writeLine("skinparam usecaseBorderColor black");
        writeLine("skinparam usecaseArrowColor LightSlateGray");
        writeLine("skinparam artifactBorderColor black");
        writeLine("skinparam artifactArrowColor LightSlateGray");
        writeLine("title Chart Map for " + chart.getNameFull());
    }

    /**
     * Writes a PlantUML footer with a time stamp and a reference to this
     * class
     *
     * @throws IOException      IOException
     */
    public void printFooter() throws IOException {
        writeLine("center footer Generated on " + getCurrentDateTime() + " by " + this.getClass().getCanonicalName() + "\\nhttps://github.com/Alfresco/alfresco-anaxes-chartmap");
        writeLine("@enduml");
    }

    /**
     * Writes a line that shows the dependency of one Helm Chart on another
     *
     * @param   parentChart     the parent Helm Chart
     * @param   dependentChart  a Helm Chart on which the parent Helm Chart depends
     * @throws  IOException     IOException
     */
    public void printChartToChartDependency(HelmChart parentChart, HelmChart dependentChart) throws IOException {
        writeLine(getNameAsPlantUmlReference(parentChart.getNameFull()) + "--->" + getNameAsPlantUmlReference(dependentChart.getNameFull()) + ":depends on");
    }

    /**
     * Writes a line that shows the dependency of a Chart on a Container
     *
     * @param   chart           a Helm Chart
     * @param   container       a Docker Container on which the Helm Chart depends
     * @throws  IOException     IOException
     */
    public void printChartToContainerDependency(HelmChart chart, HelmDeploymentContainer container) throws IOException {
        writeLine(getNameAsPlantUmlReference(chart.getNameFull()) + "--->" + getNameAsPlantUmlReference(container.getImage()) + ":uses");
    }


    /**
     * Writes a line to depict a Helm Chart
     * @param   chart   a Helm Chart
     * @throws  IOException     IOException
     */
    public void printChart(HelmChart chart) throws IOException {
        writeLine("artifact \"" + chart.getNameFull() + getComponentBody(chart) + "\" as " + getNameAsPlantUmlReference(chart.getNameFull()) + " " + getChartArtifactColor(chart));
    }

    /**
     * Writes a line to depict a Docker Container
     */
    public void printContainer(HelmDeploymentContainer container)  throws IOException {
        //     image: "quay.io/alfresco/service-sync:2.2-SNAPSHOT"
        writeLine("usecase \"" + getContainerBody(container) + "\" as " + getNameAsPlantUmlReference(container.getImage()) + " " + getContainerArtifactColor(container));
    }

    /**
     * Writes a comment line
     *
     * @param   comment the comment to be written
     * @throws  IOException     IOException
     */
    public void printComment(String comment) throws IOException {
        writer.write("'" + comment + "\n");
    }

    /**
     * Returns the text to use in a PlantUML artifact that
     * describes a Helm Chart
     *
     * @param   chart   a Helm Chart
     * @return          text that can be used for the body of
     *                  a PlantUML artifact
     */
    private String getComponentBody(HelmChart chart) {
        String body = getSeparator();
        body += "\\t" + chart.getName();
        body += getSeparator();
        body += "\\t" + chart.getVersion();
        body += getSeparator();
        body += "\\t" + getMaintainers(chart.getMaintainers());
        body += getSeparator();
        body += "\\t" + getKeywords(chart.getKeywords());
        return body;
    }

    /**
     * Returns the text to use in a PlantUML artifact that
     * describes a Docker Container
     *
     * @param   container   a Docker Container
     * @return               text that can be used for the body of
     *                       a PlantUML artifact
     */
    private String getContainerBody(HelmDeploymentContainer container) {
        String image = container.getImage();
        String imageName=null;
        String body="Image";
        body += getSeparator();
        String repoHost="DockerHub";
        if (container.getImage().contains("/")) {
            repoHost = image.substring(0,image.indexOf('/'));
        }
        body += "\\t" + repoHost;
        body += getSeparator();
        if (image.contains("/") && image.contains(":")) {
            imageName = image.substring(image.indexOf('/')+1,image.indexOf(':'));
        }
        else if (image.contains(":")) {
            imageName = image.substring(0,image.indexOf(':'));
        }
        body += "\\t" + imageName;
        body += getSeparator();
        String version="?";
        if (container.getImage().contains(":")) {
            version = image.substring(image.indexOf(':')+1,image.length());
        }
        body += "\\t" + version;
        return body;
    }

    /**
     * Returns a PlantUML separator
     *
     * @return      PlantUML text for a separator
     */
    private String getSeparator() {
        return "\\n====\\n";
    }

    /**
     * Get the maintainers for a Helm Chart, nicely formatted
     *
     * @param   m   an array of maintainers discovered in a Helm Chart
     * @return      a formatted String of the maintainers separated by
     *              commas
     */
    private String getMaintainers(HelmMaintainer[] m) {
        String maintainers = "Maintainers: ";
        boolean first = true;
        if (m != null) {
            for (HelmMaintainer hm : m) {
                if (first) {
                    maintainers += hm.getName();
                    first = false;
                } else {
                    maintainers += ", " + hm.getName();
                }
            }
        }
        return maintainers;
    }

    /**
     * Get the keywords for a Helm Chart, nicely formatted
     *
     * @param   k   an array of keywords discovered from a Helm Chart
     * @return      a formatted String of the keywords separated by
     *              commas
     */
    private String getKeywords(String[] k) {
        String keywords = "Keywords: ";
        boolean first = true;
        if (k != null) {
            for (String aKeyword : k) {
                if (first) {
                    keywords += aKeyword;
                    first = false;
                } else {
                    keywords += ", " + aKeyword;
                }
            }
        }
        return keywords;
    }

    /**
     *
     * Get the name of a Helm artifact as a name that obeys PlantUML
     * rules
     *
     * @param   s   the full name of the the artifact
     * @return      the name of the artifact where the characters
     *              that would violate PlantUML naming rules are replaced
     *              with underscores
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
     * @param   h   a HelmChart for which you want a background color
     * @return      a PlantUML color attribute chosen from the colors
     *              table
     */

    private String getChartArtifactColor(HelmChart h) {
        int hashValue = hashHelmChartName(h);
        return "#" + colors[hashValue];
    }

    private String getContainerArtifactColor(HelmDeploymentContainer c) {
        int hashValue = hashHelmContainerName(c);
        return "#" + colors[hashValue];
    }

    /**
     *
     * @param   h   a HelmChart from which a hash code will be
     *              generated (for the purpose of indexing into
     *              the colors array).   Only the name of the
     *              Helm chart is used to create the has because
     *              the main point of choosing a color is to associate
     *              visually a group of related Helm Charts in the
     *              diagram (e.g. all the Postgresql Charts may be
     *              colored as 'Coral').
     *
     * @return      a calculated hash value aa an unsigned int
     */
    private int hashHelmChartName(HelmChart h) {
        int hashCode =  (h.getName().hashCode() * Integer.MAX_VALUE) / (Integer.MAX_VALUE / (getColors().length) * 2);
        hashCode = Math.abs(hashCode);
        return hashCode;
    }

    /**
     *
     * @param   c  a HelmDeploymentContainer from which a hash code will be
     *              generated (for the purpose of indexing into
     *              the colors array).   Only the name of the
     *              Containers is used to create the has because
     *              the main point of choosing a color is to associate
     *              visually a group of related Containers in the
     *              diagram (e.g. all the Postgresql Images may be
     *              colored as 'Chocolate').
     *
     * @return      a calculated hash value aa an unsigned int
     */
    private int hashHelmContainerName(HelmDeploymentContainer c) {
        String[] s = c.getImage().split(":");
        String baseName = s[0];
        int hashCode =  (baseName.hashCode() * Integer.MAX_VALUE) / (Integer.MAX_VALUE / (getColors().length) * 2);
        hashCode = Math.abs(hashCode);
        return hashCode;
    }

    /**
     *              Get the colors array
     * @return      the colors array
     */

    private String[] getColors() {
        return colors;
    }

    /**
     * Initializes the colors array with values that PlantUML can use to
     * decorate the generated diagram.
     *
     * The color values are derived from http://plantuml.com/color
     *
     * Color values that are too dark to use with black text are
     * commented out so they are ineligible.
     *
     * PlantUML does support using other than black text so if you decide
     * to use a different color (using skinparam) you may decide to use these
     * darker colors.  A matter of taste.
     *
     * But note that your choice of text color applies to the
     * whole file.
     */

    private void initializeColors() {
        colors = new String[]{
                "AliceBlue",
                "AntiqueWhite",
                "Aqua",
                "Aquamarine",
                "Azure",
                "Beige",
                "Bisque",
                //"Black",
                "BlanchedAlmond",
                //"Blue",
                //"BlueViolet",
                //"Brown",
                "BurlyWood",
                "CadetBlue",
                "Chartreuse",
                "Chocolate",
                "Coral",
                "CornflowerBlue",
                "Cornsilk",
                //"Crimson",
                "Cyan",
                //"DarkBlue",
                //"DarkCyan",
                "DarkGoldenRod",
                //"DarkGray",
                //"DarkGreen",
                //"DarkGrey",
                //"DarkKhaki",
                //"DarkMagenta",
                //"DarkOliveGreen",
                //"DarkOrchid",
                //"DarkRed",
                "DarkSalmon",
                "DarkSeaGreen",
                //"DarkSlateBlue",
                //"DarkSlateGray",
                //"DarkSlateGrey",
                "DarkTurquoise",
                //"DarkViolet",
                "Darkorange",
                //"DeepPink",
                "DeepSkyBlue",
                //"DimGray",
                //"DimGrey",
                "DodgerBlue",
                //"FireBrick",
                "FloralWhite",
                //"ForestGreen",
                "Fuchsia",
                "Gainsboro",
                "GhostWhite",
                "Gold",
                "GoldenRod",
                //"Gray",
                //"Green",
                "GreenYellow",
                //"Grey",
                "HoneyDew",
                "HotPink",
                "IndianRed",
                //"Indigo",
                "Ivory",
                "Khaki",
                "Lavender",
                "LavenderBlush",
                "LawnGreen",
                "LemonChiffon",
                "LightBlue",
                "LightCoral",
                "LightCyan",
                "LightGoldenRodYellow",
                "LightGray",
                "LightGreen",
                "LightGrey",
                "LightPink",
                "LightSalmon",
                "LightSeaGreen",
                "LightSkyBlue",
                "LightSlateGray",
                "LightSlateGrey",
                "LightSteelBlue",
                "LightYellow",
                "Lime",
                "LimeGreen",
                "Linen",
                //"Magenta",
                "Maroon",
                "MediumAquaMarine",
                //"MediumBlue",
                "MediumOrchid",
                "MediumPurple",
                "MediumPurple",
                "MediumSeaGreen",
                //"MediumSlateBlue",
                "MediumSpringGreen",
                "MediumTurquoise",
                //"MediumVioletRed",
                //"MidnightBlue",
                "MintCream",
                "MistyRose",
                "Moccasin",
                "NavajoWhite",
                //"Navy",
                "OldLace",
                "Olive",
                "OliveDrab",
                "Orange",
                "OrangeRed",
                //"Orchid",
                "PaleGoldenRod",
                "PaleGreen",
                "PaleTurquoise",
                "PaleVioletRed",
                "PapayaWhip",
                "PeachPuff",
                "Peru",
                "Pink",
                "Plum",
                "PowderBlue",
                //"Purple",
                "Red",
                "RosyBrown",
                "RoyalBlue",
                //"SaddleBrown",
                "Salmon",
                "SandyBrown",
                //"SeaGreen",
                "SeaShell",
                //"Sienna",
                "Silver",
                "SkyBlue",
                //"SlateBlue",
                //"SlateGray",
                //"SlateGrey",
                "Snow",
                "SpringGreen",
                //"SteelBlue",
                "Tan",
                //"Teal",
                "Thistle",
                "Tomato",
                "Turquoise",
                "Violet",
                "Wheat",
                "White",
                "WhiteSmoke",
                "Yellow",
                "YellowGreen"
        };
    }
}
