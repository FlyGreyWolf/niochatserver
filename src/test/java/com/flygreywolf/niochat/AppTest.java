package com.flygreywolf.niochat;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    private static volatile AtomicInteger msgId = new AtomicInteger(0);
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {

        Random random = new Random();
        int randomInt = random.nextInt(101);


        for(int i=0;i<100;i++) {
            System.out.println(random.nextInt(101));
        }
        assertTrue( true );
    }
}
