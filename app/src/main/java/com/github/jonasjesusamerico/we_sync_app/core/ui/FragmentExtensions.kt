package com.github.jonasjesusamerico.we_sync_app.auth.ui

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.github.jonasjesusamerico.we_sync_app.AppContainer
import com.github.jonasjesusamerico.we_sync_app.WeSyncApplication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

internal fun Fragment.appContainer(): AppContainer =
    (requireActivity().application as WeSyncApplication).container

/**
 * Navigates with [actionId] only while this fragment is still the current destination, so a state
 * delivered twice cannot trigger the same action from the next screen (which would throw).
 */
internal fun Fragment.navigateFrom(@IdRes destinationId: Int, @IdRes actionId: Int, args: Bundle? = null) {
    val navController = findNavController()
    if (navController.currentDestination?.id == destinationId) navController.navigate(actionId, args)
}

/** Collects [flow] while the view is at least STARTED. */
internal fun <T> Fragment.collectWhileStarted(flow: Flow<T>, action: (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { action(it) }
        }
    }
}
