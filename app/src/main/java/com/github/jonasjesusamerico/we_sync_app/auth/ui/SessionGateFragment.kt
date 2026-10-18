package com.github.jonasjesusamerico.we_sync_app.auth.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.github.jonasjesusamerico.we_sync_app.R
import com.github.jonasjesusamerico.we_sync_app.core.ui.appContainer
import com.github.jonasjesusamerico.we_sync_app.core.ui.collectWhileStarted
import com.github.jonasjesusamerico.we_sync_app.core.ui.navigateFrom
import com.github.jonasjesusamerico.we_sync_app.databinding.FragmentSessionGateBinding

class SessionGateFragment : Fragment() {

    private var _binding: FragmentSessionGateBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: SessionGateViewModel by viewModels {
        viewModelFactory { initializer { SessionGateViewModel(appContainer().authRepository) } }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSessionGateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonRetry.setOnClickListener { viewModel.retry() }

        collectWhileStarted(viewModel.state) { state ->
            binding.progress.isVisible = state == SessionGateState.LOADING
            binding.groupOffline.isVisible = state == SessionGateState.OFFLINE
            when (state) {
                SessionGateState.AUTHENTICATED -> navigateFrom(R.id.sessionGateFragment, R.id.action_sessionGate_to_home)
                SessionGateState.UNAUTHENTICATED -> navigateFrom(R.id.sessionGateFragment, R.id.action_sessionGate_to_login)
                SessionGateState.LOADING, SessionGateState.OFFLINE -> Unit
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
