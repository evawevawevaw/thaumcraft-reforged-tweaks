package dev.reforgedtweaks.client.book;

import org.junit.Test;
import static org.junit.Assert.*;

public class BackgroundRegionTest {
    private BackgroundRegion centred(int w, int h, double dx, double dy, int columns, int rows) {
        return BackgroundRegion.at(columns * 12 + 11 - w / 2.0 + dx,
            rows * 12 + 11 - h / 2.0 + dy, 0, columns, 0, rows, w, h);
    }
    @Test public void identicalDragsHaveTheSameVisibleSpeedAtEveryBrowserSize() {
        var start = centred(224,196,0,0,60,50);
        var end = centred(224,196,10,10,60,50);
        double referenceX=end.u()-start.u(), referenceY=end.v()-start.v();
        for(int[] size:new int[][]{{96,81},{224,196},{480,426},{800,700}}) {
            var a=centred(size[0],size[1],0,0,60,50);
            var b=centred(size[0],size[1],10,10,60,50);
            assertEquals(referenceX,(b.u()-a.u())*size[0]/224.0,.001);
            assertEquals(referenceY,(b.v()-a.v())*size[1]/196.0,.001);
        }
    }
    @Test public void smallTabsCannotAmplifyDragIntoFullTextureTravel() {
        for(int[] size:new int[][]{{224,196},{480,426},{800,700}}) {
            var a=centred(size[0],size[1],0,0,8,8);
            var b=centred(size[0],size[1],10,10,8,8);
            assertEquals(5,(b.u()-a.u())*size[0]/224.0,.001);
            assertEquals(5,(b.v()-a.v())*size[1]/196.0,.001);
        }
    }
    @Test public void fractionalCameraMotionIsNotRoundedToTwoTexels() {
        var a=centred(480,426,0,0,60,50);
        var b=centred(480,426,.125,.125,60,50);
        var c=centred(480,426,.25,.25,60,50);
        assertTrue(b.u()>a.u() && c.u()>b.u());
        assertTrue(b.v()>a.v() && c.v()>b.v());
        assertTrue(c.u()-a.u()<1); assertTrue(c.v()-a.v()<1);
    }
    @Test public void extremePanClampsRatherThanWrappingOrRepeating() {
        for(double position:new double[]{-1e12,-20,0,100,1e12}) {
            var b=BackgroundRegion.at(position,position,0,60,0,50,480,426);
            assertTrue(b.u()>=0 && b.u()+b.width()<=512);
            assertTrue(b.v()>=0 && b.v()+b.height()<=512);
        }
        assertEquals(0,BackgroundRegion.at(-1e12,-1e12,0,60,0,50,480,426).u(),0);
        assertEquals(288,BackgroundRegion.at(1e12,1e12,0,60,0,50,480,426).u(),0);
    }
    @Test public void collapsedAndInvalidSpansStillProduceFiniteBoundedUvs() {
        for(int[] bounds:new int[][]{{1,1},{5,1},{Integer.MIN_VALUE,Integer.MAX_VALUE}}) {
            var b=BackgroundRegion.at(0,0,bounds[0],bounds[1],bounds[0],bounds[1],0,0);
            assertTrue(Float.isFinite(b.u()) && Float.isFinite(b.v()));
            assertTrue(b.u()>=0 && b.u()+b.width()<=512);
            assertTrue(b.v()>=0 && b.v()+b.height()<=512);
        }
    }
    @Test public void everyCategorySizeUsesTheSameSpeedLimitForBothAxes() {
        for(int span:new int[]{0,1,8,20,60,1000}) {
            var a=centred(600,500,0,0,span,span);
            var b=centred(600,500,1,1,span,span);
            assertTrue((b.u()-a.u())*600/224.0 <= .501);
            assertTrue((b.v()-a.v())*500/196.0 <= .501);
        }
    }
}
