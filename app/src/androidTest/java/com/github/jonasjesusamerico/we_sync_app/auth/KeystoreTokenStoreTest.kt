package com.github.jonasjesusamerico.we_sync_app.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeystoreTokenStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = KeystoreTokenStore(context)

    @After
    fun tearDown() {
        store.clear()
    }

    @Test
    fun saveAndRead_roundTrip() {
        store.saveRefreshToken("refresh-token-value")

        assertEquals("refresh-token-value", KeystoreTokenStore(context).readRefreshToken())
    }

    @Test
    fun storedValue_isNotPlainText() {
        store.saveRefreshToken("refresh-token-value")

        val raw = context.getSharedPreferences("auth_session", Context.MODE_PRIVATE).all.values.joinToString()
        assertFalse(raw.contains("refresh-token-value"))
    }

    @Test
    fun clear_removesToken() {
        store.saveRefreshToken("refresh-token-value")
        store.clear()

        assertNull(store.readRefreshToken())
    }

    @Test
    fun corruptedValue_isTreatedAsNoSession() {
        context.getSharedPreferences("auth_session", Context.MODE_PRIVATE)
            .edit().putString("refresh_token", "not-valid").commit()

        assertNull(store.readRefreshToken())
    }
}
