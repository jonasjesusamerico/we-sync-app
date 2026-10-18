package com.github.jonasjesusamerico.we_sync_app.core.ui.drawer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerMenuTest {

    @Test
    fun collapsedByDefault_onlySectionsAreVisible() {
        val rows = DrawerMenu.rows(expanded = emptySet(), selected = DrawerAction.HOME)

        assertTrue(rows.all { it is DrawerRow.Section })
        assertEquals(listOf("home", "purchases", "account"), rows.map { (it as DrawerRow.Section).section.key })
        assertTrue((rows.first() as DrawerRow.Section).isSelected)
    }

    @Test
    fun expandedSection_showsItsChildrenRightAfterIt() {
        val rows = DrawerMenu.rows(expanded = setOf("purchases"), selected = DrawerAction.NEW_PURCHASE)

        assertEquals(4, rows.size)
        assertTrue((rows[1] as DrawerRow.Section).isExpanded)
        val child = rows[2] as DrawerRow.Child
        assertEquals(DrawerAction.NEW_PURCHASE, child.child.action)
        assertTrue(child.isSelected)
    }

    @Test
    fun toggle_addsAndRemoves() {
        val expanded = DrawerMenu.toggle(emptySet(), "account")
        assertEquals(setOf("account"), expanded)
        assertEquals(emptySet<String>(), DrawerMenu.toggle(expanded, "account"))
    }

    @Test
    fun directSectionsAreNotExpandable() {
        val rows = DrawerMenu.rows(expanded = setOf("home"), selected = null)

        assertEquals(3, rows.size)
    }
}
