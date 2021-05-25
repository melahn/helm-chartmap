package com.melahn.util.helm.model;

import java.util.Objects;

public class HelmDeploymentContainer {
    private String name;
    private String image;
    private String imagePullPolicy;
    /**
     * The _parent property is not found in the yaml file but rather is inserted //
     * during template processing. It records the chart that uses this container //
     * since some charts are common across a deployment but differ where used in //
     * the imageTag property
     */
    private HelmChart parent; // not in the model

    public String getImagePullPolicy() {
        return imagePullPolicy;
    }

    public void setImagePullPolicy(String imagePullPolicy) {
        this.imagePullPolicy = imagePullPolicy;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    /**
     * the equals and hashCode overrides are needed to make comparisons when adding
     * to arrays thatare used for the chart maps since there are cases where the
     * same container may be used by multiple templates that are used by a helm
     * chart. For an example, see the StateFulSets in wordpress:8.1.2.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        HelmDeploymentContainer that = (HelmDeploymentContainer) o;
        return Objects.equals(name, that.name) && Objects.equals(image, that.image)
                && Objects.equals(imagePullPolicy, that.imagePullPolicy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, image, imagePullPolicy);
    }

    /* parent is not in the model */
    public HelmChart getParent() {
        return this.parent;
    }

    public void setParent(HelmChart parent) {
        this.parent = parent;
    }
}
