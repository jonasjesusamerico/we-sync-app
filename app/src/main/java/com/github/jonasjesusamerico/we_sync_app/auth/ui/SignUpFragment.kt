package com.github.jonasjesusamerico.we_sync_app.auth.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.github.jonasjesusamerico.we_sync_app.R
import com.github.jonasjesusamerico.we_sync_app.core.ui.appContainer
import com.github.jonasjesusamerico.we_sync_app.core.ui.collectWhileStarted
import com.github.jonasjesusamerico.we_sync_app.core.ui.navigateFrom
import com.github.jonasjesusamerico.we_sync_app.databinding.FragmentSignUpBinding

class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: SignUpViewModel by viewModels {
        viewModelFactory { initializer { SignUpViewModel(appContainer().authRepository) } }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonSignUp.setOnClickListener { submit() }
        binding.inputPasswordConfirmation.setOnEditorActionListener { _, actionId, _ ->
            (actionId == EditorInfo.IME_ACTION_DONE).also { if (it) submit() }
        }
        binding.buttonGoToLogin.setOnClickListener { goToLogin(accountCreated = false) }

        collectWhileStarted(viewModel.state) { state ->
            binding.layoutEmail.error = state.emailError?.let { getString(it.textRes()) }
            binding.layoutPassword.error = state.passwordError?.let { getString(it.textRes()) }
            binding.layoutPasswordConfirmation.error = state.confirmationError?.let { getString(it.textRes()) }
            binding.textMessage.showAuthMessage(state.message)
            binding.progress.isVisible = state.isLoading
            binding.buttonSignUp.isEnabled = !state.isLoading
            binding.buttonGoToLogin.isEnabled = !state.isLoading
            when {
                state.isAuthenticated -> navigateFrom(R.id.signUpFragment, R.id.action_signUp_to_home)
                state.isAccountCreated -> goToLogin(accountCreated = true)
            }
        }
    }

    private fun submit() {
        viewModel.submit(
            binding.inputEmail.text.toString(),
            binding.inputPassword.text.toString(),
            binding.inputPasswordConfirmation.text.toString(),
        )
    }

    private fun goToLogin(accountCreated: Boolean) {
        navigateFrom(
            R.id.signUpFragment,
            R.id.action_signUp_to_login,
            bundleOf(
                LoginFragment.ARG_EMAIL to binding.inputEmail.text.toString().trim(),
                LoginFragment.ARG_ACCOUNT_CREATED to accountCreated,
            ),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
