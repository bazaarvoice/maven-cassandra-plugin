package com.bazaarvoice.core.quality.testng.listener;

import com.google.common.collect.Lists;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang.StringUtils;
import org.testng.*;

import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractTestCaseListener extends TestListenerAdapter implements IMethodInterceptor {

    private static final String PROPERTY_DISABLE_SYSTEM_OUT = "disableSystemOut";
    private static final String PROPERTY_DISABLE_SYSTEM_OUT_DEFAULT = "false";

    private static final String PROPERTY_DISABLE_SYSTEM_ERR = "disableSystemErr";
    private static final String PROPERTY_DISABLE_SYSTEM_ERR_DEFAULT = "false";

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance();

    private PrintStream _systemOut;
    private PrintStream _systemErr;

    private int _testCount;
    private Set<String> _executedTestNames;

    public void onStart(ITestContext iTestContext) {
        super.onStart(iTestContext);

        _systemOut = System.out;
        _systemErr = System.err;

        boolean disableSystemOut = Boolean.parseBoolean(System.getProperty(PROPERTY_DISABLE_SYSTEM_OUT, PROPERTY_DISABLE_SYSTEM_OUT_DEFAULT));
        if (disableSystemOut) {
            System.setOut(new PrintStream(new NullOutputStream()));
        }

        boolean disableSystemErr = Boolean.parseBoolean(System.getProperty(PROPERTY_DISABLE_SYSTEM_ERR, PROPERTY_DISABLE_SYSTEM_ERR_DEFAULT));
        if (disableSystemErr) {
            System.setErr(new PrintStream(new NullOutputStream()));
        }

        _testCount = iTestContext.getAllTestMethods().length;
        _executedTestNames = new HashSet<String>(_testCount);
    }

    public void onFinish(ITestContext iTestContext) {
        super.onFinish(iTestContext);

        long time = iTestContext.getEndDate().getTime() - iTestContext.getStartDate().getTime();
        _systemOut.println();
        _systemOut.println("===============================================");
        _systemOut.println("Suite Time : " + NUMBER_FORMAT.format(time / 1000 / 60) + " min");

        System.setOut(_systemOut);
        System.setErr(_systemErr);
    }

    @Override
    public void onTestStart(ITestResult tr) {
        super.onTestStart(tr);

        //Some tests are run multiple times because they have a dataProvider which alters the parameters
        //It doesn't look like TestNG exposes the number of times a test will be invoked via this method until after it's actually been executed
        //So, the best we can do to count is to keep track of how many unique test names have been invoked.
        //This number will also match what came back as the _testCount, so we'll never end up with currentCount > totalCount.
        _executedTestNames.add(getTestName(tr));

        log("STARTED", tr, false);
    }

    @Override
    public void onTestFailure(ITestResult tr) {
        super.onTestFailure(tr);
        log("FAILED", tr, true);
    }

    @Override
    public void onTestSkipped(ITestResult tr) {
        super.onTestSkipped(tr);
        log("SKIPPED", tr, true);
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult tr) {
        super.onTestFailedButWithinSuccessPercentage(tr);
        log("FAILED", tr, true);
    }

    @Override
    public void onTestSuccess(ITestResult tr) {
        super.onTestSuccess(tr);
        log("PASSED", tr, true);
    }

    private void log(String result, ITestResult tr, boolean printTime) {
        int numDigits = Integer.toString(_testCount).length();
        String testCountString = "(" + StringUtils.leftPad(Integer.toString(_executedTestNames.size()), numDigits, '0') + "/" + _testCount + ")";
        String resultString = StringUtils.rightPad(result + ":", 8, ' ');

        long time = tr.getEndMillis() - tr.getStartMillis();
        String timeString = NUMBER_FORMAT.format(time) + " ms";

        _systemOut.print(testCountString + " " + resultString + " " + getTestName(tr));
        if (printTime) {
            _systemOut.print("\t" + timeString);
        }
        _systemOut.println();
    }

    private static String getTestName(ITestResult tr) {
        String className = StringUtils.substringAfterLast(tr.getTestClass().getName(), ".");
        String testName = tr.getName();
        return className + "." + testName;
    }

    /**
     * This method will intercept test method search criteria.
     * If the user do not provide any value for 'groups' system property or
     * the user asks for TestGroups.TEST_GROUP_ALL group
     * then all the test methods will be returned.
     * If the user has given a specific test group (or groups separated by comma)
     * then this will return all the test methods matching the given groups.
     * This will always add the test methods belonging to TestGroups.TEST_GROUP_CLEAN group
     * at the end of the list, so that cleanup test cases will be run last (except when the returned list is empty).
     */
    @Override
    public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
        List<IMethodInstance> allMethods = Lists.newArrayList();
        List<IMethodInstance> cleanMethods = Lists.newArrayList();

        // divide methods into clean and regular test methods
        for (IMethodInstance m : methods) {
            if (isCleanMethod(m)) {
                cleanMethods.add(m);
            } else {
                allMethods.add(m);
            }
        }

        // add clean methods to end of test methods
        allMethods.addAll(cleanMethods);

        // reset counter
        _testCount = allMethods.size();

        return allMethods;
    }

    private boolean isCleanMethod(IMethodInstance mi) {
        return Arrays.asList(mi.getMethod().getGroups()).contains(getCleanGroupName());
    }

    /**
     * Subclasses must implement to describe the name of the "clean" test group
     */
    protected abstract String getCleanGroupName();
}

