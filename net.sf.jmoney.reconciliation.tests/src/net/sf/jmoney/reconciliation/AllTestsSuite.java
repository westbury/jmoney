package net.sf.jmoney.reconciliation;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTestsSuite {
    public static Test suite() {
        TestSuite suite = new TestSuite("All Tests");
        suite.addTest(AllTests.suite());
        return suite;
    }
}