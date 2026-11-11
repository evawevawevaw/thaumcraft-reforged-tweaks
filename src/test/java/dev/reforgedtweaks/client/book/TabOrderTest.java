package dev.reforgedtweaks.client.book;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class TabOrderTest {
    @Test public void engineeringCanMoveToFirstWithoutLosingOtherTabs() {
        assertEquals(List.of("ENGINEERING","BASICS","ALCHEMY"),TabOrder.moveBefore(List.of("BASICS","ALCHEMY","ENGINEERING"),"ENGINEERING","BASICS"));
    }
    @Test public void bottomHalfDropCanMoveTheFirstTabToLast() {
        assertEquals(List.of("B","C","A"),TabOrder.move(List.of("A","B","C"),"A","C",true));
    }
    @Test public void temporarilyHiddenAddonOrderSurvivesAndNewTabsAppend() {
        assertEquals(List.of("ADDON","BASICS","NEW"),TabOrder.reconcile(List.of("ADDON","BASICS","ADDON"),List.of("BASICS","NEW")));
    }
    @Test public void cancelledOrInvalidDropLeavesOriginalUnchanged() {
        var original=List.of("A","B","C");
        assertEquals(original,TabOrder.moveBefore(original,"A","A"));
        assertEquals(original,TabOrder.moveBefore(original,"X","A"));
        assertEquals(original,TabOrder.moveBefore(original,"A","X"));
    }
}
