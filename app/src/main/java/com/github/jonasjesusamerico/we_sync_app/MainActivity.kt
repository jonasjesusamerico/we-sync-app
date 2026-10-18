package com.github.jonasjesusamerico.we_sync_app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.github.jonasjesusamerico.we_sync_app.auth.ui.SessionViewModel
import com.github.jonasjesusamerico.we_sync_app.core.ui.drawer.DrawerAction
import com.github.jonasjesusamerico.we_sync_app.core.ui.drawer.DrawerAdapter
import com.github.jonasjesusamerico.we_sync_app.core.ui.drawer.DrawerMenu
import com.github.jonasjesusamerico.we_sync_app.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val container get() = (application as WeSyncApplication).container

    private val sessionViewModel: SessionViewModel by viewModels {
        viewModelFactory { initializer { SessionViewModel(container.authRepository) } }
    }

    private val drawerAdapter = DrawerAdapter(
        onSectionClick = { section ->
            val action = section.action
            if (action != null) onDrawerAction(action) else toggleSection(section.key)
        },
        onChildClick = { child -> onDrawerAction(child.action) },
    )
    private var expandedSections: Set<String> = emptySet()
    private var selectedAction: DrawerAction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light status bar icons: the brand header band sits behind the status bar in both themes.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets()
        setSupportActionBar(binding.toolbar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        navController = navHostFragment.navController

        // ☰ on the dashboard (top level); hidden again below on the unauthenticated top-level screens.
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.sessionGateFragment, R.id.loginFragment, R.id.homeFragment),
            binding.drawerLayout,
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        expandedSections = savedInstanceState?.getStringArrayList(KEY_EXPANDED_SECTIONS)?.toSet().orEmpty()
        setupDrawer()
        observeSignedOut()
        observeLogout()
    }

    private fun applyWindowInsets() {
        val headerTopPadding = binding.drawerHeader.root.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            binding.appBar.updatePadding(top = systemBars.top)
            binding.main.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            binding.drawerHeader.root.updatePadding(top = headerTopPadding + systemBars.top)
            binding.drawerList.updatePadding(bottom = systemBars.bottom)
            insets
        }
    }

    private fun setupDrawer() {
        binding.drawerList.adapter = drawerAdapter

        val closeDrawerOnBack = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() = binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
        onBackPressedDispatcher.addCallback(this, closeDrawerOnBack)
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: android.view.View) {
                closeDrawerOnBack.isEnabled = true
            }

            override fun onDrawerClosed(drawerView: android.view.View) {
                closeDrawerOnBack.isEnabled = false
            }
        })

        // Registered after setupActionBarWithNavController, so it runs after NavigationUI updated the toolbar.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isUnauthenticated = destination.id in unauthenticatedDestinations
            binding.drawerLayout.setDrawerLockMode(
                if (isUnauthenticated) DrawerLayout.LOCK_MODE_LOCKED_CLOSED else DrawerLayout.LOCK_MODE_UNLOCKED,
            )
            if (isUnauthenticated) supportActionBar?.setDisplayHomeAsUpEnabled(false)
            selectedAction = when (destination.id) {
                R.id.homeFragment -> DrawerAction.HOME
                R.id.purchaseFormFragment -> DrawerAction.NEW_PURCHASE
                else -> selectedAction
            }
            renderDrawer()
        }
    }

    private fun renderDrawer() {
        drawerAdapter.submitList(DrawerMenu.rows(expandedSections, selectedAction))
    }

    private fun toggleSection(key: String) {
        expandedSections = DrawerMenu.toggle(expandedSections, key)
        renderDrawer()
    }

    private fun onDrawerAction(action: DrawerAction) {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        val current = navController.currentDestination?.id
        when (action) {
            DrawerAction.HOME -> if (current != R.id.homeFragment) navController.popBackStack(R.id.homeFragment, false)
            DrawerAction.NEW_PURCHASE ->
                if (current != R.id.purchaseFormFragment) navController.navigate(R.id.action_global_purchaseForm)

            DrawerAction.LOGOUT -> confirmLogout()
        }
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.logout_confirm_title)
            .setMessage(R.string.logout_confirm_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_logout) { _, _ -> sessionViewModel.logout() }
            .show()
    }

    private fun observeLogout() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sessionViewModel.state.collect { state ->
                    if (!state.isSignedOut) return@collect
                    goToLogin()
                    sessionViewModel.onSignedOutHandled()
                }
            }
        }
    }

    // When the session can no longer be refreshed, leave the authenticated area.
    private fun observeSignedOut() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                container.tokenRefresher.signedOut.collect { goToLogin() }
            }
        }
    }

    private fun goToLogin() {
        if (navController.currentDestination?.id !in unauthenticatedDestinations) {
            navController.navigate(R.id.action_global_login)
        }
    }

    private val unauthenticatedDestinations = setOf(R.id.sessionGateFragment, R.id.loginFragment, R.id.signUpFragment)

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(KEY_EXPANDED_SECTIONS, ArrayList(expandedSections))
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private companion object {
        const val KEY_EXPANDED_SECTIONS = "drawer_expanded_sections"
    }
}
