package kr.ac.knu.odego;

import org.junit.Test;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {

    @Test
    public void objectTest() throws Exception {
        String a = "aa";
        String aa = a;

        a = "bb";
        System.out.println(aa);
    }


}