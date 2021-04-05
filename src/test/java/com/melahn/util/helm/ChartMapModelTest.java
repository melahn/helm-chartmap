package com.melahn.util.helm;

import org.junit.Test;
import static org.junit.Assert.assertSame;

import java.nio.charset.Charset;
import java.util.Random;

import com.melahn.util.helm.model.HelmDeploymentTemplate;

public class ChartMapModelTest {
    @Test
    public void testHelmDeploymentTemplate() {
        HelmDeploymentTemplate hdt = new HelmDeploymentTemplate();
        String expected = generateRandomString(10);
        hdt.setApiVersion(expected);
        assertSame(expected, hdt.getApiVersion());
        hdt.setFileName(expected);
        assertSame(expected, hdt.getFileName());
        hdt.setKind(expected);
        assertSame(expected, hdt.getKind());
        System.out.println("testHelmDeploymentTemplate test completed");
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
