package com.melahn.util.helm;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Random;

import com.melahn.util.helm.model.HelmChart;
import com.melahn.util.helm.model.HelmChartLocalCache;
import com.melahn.util.helm.model.HelmChartRepo;
import com.melahn.util.helm.model.HelmChartRepoLocal;
import com.melahn.util.helm.model.HelmChartReposLocal;
import com.melahn.util.helm.model.HelmDeploymentContainer;
import com.melahn.util.helm.model.HelmDeploymentSpec;
import com.melahn.util.helm.model.HelmDeploymentSpecTemplate;
import com.melahn.util.helm.model.HelmDeploymentSpecTemplateSpec;
import com.melahn.util.helm.model.HelmDeploymentTemplate;
import com.melahn.util.helm.model.HelmRequirement;

public class ChartMapModelTest {

    static final String EXPECTED = generateRandomString(10);
    static final String NOTEXPECTED = EXPECTED.concat(EXPECTED);
    
    @Test
    public void testHelmChartLocalCache() {
        HelmChartLocalCache hclc = new HelmChartLocalCache();
        hclc.setApiVersion(EXPECTED);
        assertSame(EXPECTED, hclc.getApiVersion());
        HashMap<String,HelmChart[]> e = new HashMap<String,HelmChart[]>();
        hclc.setEntries(e);
        assertSame(e, hclc.getEntries());
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    public void testHelmChartRepo() {
        HelmChartRepo hcr = new HelmChartRepo();
        hcr.setApiVersion(EXPECTED);
        assertSame(EXPECTED, hcr.getApiVersion());
        hcr.setGenerated(EXPECTED);
        assertSame(EXPECTED, hcr.getGenerated());
        HashMap<String,HelmChart[]> e = new HashMap<String,HelmChart[]>();
        hcr.setEntries(e);
        assertSame(e, hcr.getEntries());
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    public void testHelmChartReposLocal() {
        HelmChartReposLocal hcrl = new HelmChartReposLocal();
        hcrl.setApiVersion(EXPECTED);
        assertSame(EXPECTED, hcrl.getApiVersion());
        hcrl.setGenerated(EXPECTED);
        assertSame(EXPECTED, hcrl.getGenerated()); 
        HelmChartRepoLocal[] r = new HelmChartRepoLocal[1]; 
        hcrl.setRepos(r);
        assertSame(r, hcrl.getRepositories());     
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    public void testHelmDeploymentContainer() {
        HelmDeploymentContainer hdc = new HelmDeploymentContainer();
        hdc.setImage(EXPECTED);
        assertSame(EXPECTED, hdc.getImage());
        hdc.setImagePullPolicy(EXPECTED);
        assertSame(EXPECTED, hdc.getImagePullPolicy());
        hdc.setName(EXPECTED);
        assertSame(EXPECTED, hdc.getName());
        HelmDeploymentContainer hdc2 = new HelmDeploymentContainer();
        hdc2.setImage(EXPECTED);
        hdc2.setImagePullPolicy(EXPECTED);
        hdc2.setName(EXPECTED);
        boolean e = hdc.equals(hdc2);
        assertTrue(e);
        e = hdc.equals(hdc);
        assertTrue(e);
        hdc2.setImage(NOTEXPECTED);
        e = hdc.equals(hdc2);
        assertFalse(e);
        hdc2.setImage(EXPECTED);
        hdc2.setImagePullPolicy(NOTEXPECTED);
        e = hdc.equals(hdc2);
        assertFalse(e);
        hdc2.setImagePullPolicy(EXPECTED);
        hdc2.setName(NOTEXPECTED);
        e = hdc.equals(hdc2);
        assertFalse(e);
        hdc.equals(null);
        e = hdc.equals(hdc2);
        assertFalse(e);
        e = hdc.equals(new Object());
        assertFalse(e);
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    public void testHelmDeploymentSpec() {
        HelmDeploymentSpec hds = new HelmDeploymentSpec();
        hds.setReplicas(EXPECTED);
        assertSame(EXPECTED, hds.getReplicas());
        HelmDeploymentSpecTemplate hdst = new HelmDeploymentSpecTemplate();
        hds.setTemplate(hdst);
        assertSame(hdst, hds.getTemplate());
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    public void testHelmDeploymentSpecTemplateSpec() {
        HelmDeploymentSpecTemplateSpec hdsts = new HelmDeploymentSpecTemplateSpec();
        HelmDeploymentContainer[] c = new HelmDeploymentContainer[1];
        hdsts.setContainers(c);
        assertSame(c, hdsts.getContainers());
        hdsts.setHostNetwork(EXPECTED);
        assertSame(EXPECTED, hdsts.getHostNetwork());
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    public void testHelmDeploymentTemplate() {
        HelmDeploymentTemplate hdt = new HelmDeploymentTemplate();
        hdt.setApiVersion(EXPECTED);
        assertSame(EXPECTED, hdt.getApiVersion());
        hdt.setFileName(EXPECTED);
        assertSame(EXPECTED, hdt.getFileName());
        hdt.setKind(EXPECTED);
        assertSame(EXPECTED, hdt.getKind());
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    public void testHelmRequirement() {
        HelmRequirement hr = new HelmRequirement();
        hr.setCondition(EXPECTED);
        assertSame(EXPECTED, hr.getCondition());
        hr.setName(EXPECTED);
        assertSame(EXPECTED, hr.getName());
        hr.setRepository(EXPECTED);
        assertSame(EXPECTED, hr.getRepository());
        hr.setVersion(EXPECTED);
        assertSame(EXPECTED, hr.getVersion());
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }
        
    /**
     * Returns a randomly generated string. 
     * @param c the number of characters in the string you want. If
     *          invalid (less than or equal to 0) 10 is used
     * @return the generated String
     */
    private static String generateRandomString(int c) {
        byte[] array = new byte[c <= 0 ? 10 : c];
        new Random().nextBytes(array);
        return new String(array, Charset.forName("UTF-8"));
    }

}
