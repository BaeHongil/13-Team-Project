package kr.ac.knu.odego.common;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by BHI on 2016-05-23.
 */
public class ParserTest {
    private Parser mParser;

    @Before
    public void setUp() throws Exception {
        mParser = Parser.getInstance();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testGetBusStopArrInfos() throws Exception {
        /*RouteArrInfo[] routeArrInfos = mParser.getBusStopArrInfos(null, "7021025700");

        for( RouteArrInfo routeArrInfo : routeArrInfos ) {
            ArrInfo[] arrInfos = routeArrInfo.getArrInfoArray();
            for( ArrInfo arrInfo : arrInfos) {
                if( arrInfo.getMessage() == null )
                    System.out.println(arrInfo.getRemainBusStop() + " " + arrInfo.getRemainMin());
                else
                    System.out.println(arrInfo.getMessage());
            }
            System.out.println();
        }*/
    }

    @Test
    public void testIsNum() throws Exception {
        assertThat(mParser.isNum("14"), is(true));
    }
}