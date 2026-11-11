package dev.reforgedtweaks.client.book;

import org.junit.Test;
import static org.junit.Assert.*;

public class BookGeometryTest {
    @Test public void scaleOneRetainsOriginalViewport() {
        var book = BookGeometry.fit(960, 540, 1);
        assertEquals(256, book.width()); assertEquals(230, book.height());
        assertEquals(224, book.viewWidth()); assertEquals(196, book.viewHeight());
    }
    @Test public void doubleScaleShowsMoreWorldWithoutScalingResearchUnits() {
        var book = BookGeometry.fit(960, 540, 2);
        assertEquals(512, book.width()); assertEquals(460, book.height());
        assertEquals(480, book.viewWidth()); assertTrue(book.rows() > 9);
    }
    @Test public void largeRequestsFitSmallAndWideScreensWithTabsInside() {
        for (int[] size : new int[][]{{320,240},{960,540},{1280,360},{1920,1080}}) {
            var b = BookGeometry.fit(size[0],size[1],4);
            assertTrue(b.left()-24 >= 0); assertTrue(b.top() >= 0);
            assertTrue(b.left()+b.width()+24 <= size[0]); assertTrue(b.top()+b.height() <= size[1]);
            assertTrue(b.rows()*24 <= b.height()-18);
        }
    }
    @Test public void scaleStepsStayOnDecimalGridAndRespectLimits() {
        assertEquals(.5,BookGeometry.step(.5,-1),0); assertEquals(4,BookGeometry.step(4,1),0);
        double value=1; for(int i=0;i<10;i++)value=BookGeometry.step(value,1);
        assertEquals(2,value,0); assertEquals(256,BookGeometry.fit(960,540,Double.NaN).width());
    }
}
