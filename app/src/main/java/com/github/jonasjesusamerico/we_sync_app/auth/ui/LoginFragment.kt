package com.github.jonasjesusamerico.we_sync_app.auth.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.github.jonasjesusamerico.we_sync_app.R
import com.github.jonasjesusamerico.we_sync_app.core.ui.appContainer
import com.github.jonasjesusamerico.we_sync_app.core.ui.collectWhileStarted
import com.github.jonasjesusamerico.we_sync_app.core.ui.navigateFrom
import com.github.jonasjesusamerico.we_sync_app.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels {
        viewModelFactory {
            initializer {
                val accountCreated = arguments?.getBoolean(ARG_ACCOUNT_CREATED) == true
                LoginViewModel(
                    appContainer().authRepository,
                    initialMessage = if (accountCreated) AuthMessage.ACCOUNT_CREATED else null,
                )
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            arguments?.getString(ARG_EMAIL)?.let { binding.inputEmail.setText(it) }
        }

        binding.buttonLogin.setOnClickListener { submit() }
        binding.inputPassword.setOnEditorActionListener { _, actionId, _ ->
            (actionId == EditorInfo.IME_ACTION_DONE).also { if (it) submit() }
        }
        binding.buttonGoToSignUp.setOnClickListener {
            navigateFrom(R.id.loginFragment, R.id.action_login_to_signUp)
        }

        collectWhileStarted(viewModel.state) { state ->
            binding.layoutEmail.error = state.emailError?.let { getString(it.textRes()) }
            binding.layoutPassword.error = state.passwordError?.let { getString(it.textRes()) }
            binding.textMessage.showAuthMessage(state.message)
            binding.progress.isVisible = state.isLoading
            binding.buttonLogin.isEnabled = !state.isLoading
            binding.buttonGoToSignUp.isEnabled = !state.isLoading
            if (state.isAuthenticated) navigateFrom(R.id.loginFragment, R.id.action_login_to_home)
        }
    }

    private fun submit() {
        viewModel.submit(binding.inputEmail.text.toString(), binding.inputPassword.text.toString())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        // Navigation arguments, declared in nav_graph.xml.
        const val ARG_EMAIL = "email"
        const val ARG_ACCOUNT_CREATED = "accountCreated"
    }
}
