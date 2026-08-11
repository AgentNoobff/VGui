package me.agent.vgui.core.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaginationMathTest {

    @Test
    void pageCount() {
        assertEquals(1, PaginationImpl.pageCount(0, 21));
        assertEquals(1, PaginationImpl.pageCount(1, 21));
        assertEquals(1, PaginationImpl.pageCount(21, 21));
        assertEquals(2, PaginationImpl.pageCount(22, 21));
        assertEquals(5, PaginationImpl.pageCount(100, 21));
        assertEquals(1, PaginationImpl.pageCount(100, 0)); // unconfigured region
    }

    @Test
    void clampPage() {
        assertEquals(0, PaginationImpl.clampPage(-5, 3));
        assertEquals(0, PaginationImpl.clampPage(0, 3));
        assertEquals(2, PaginationImpl.clampPage(2, 3));
        assertEquals(2, PaginationImpl.clampPage(99, 3));
        assertEquals(0, PaginationImpl.clampPage(0, 1));
    }
}
