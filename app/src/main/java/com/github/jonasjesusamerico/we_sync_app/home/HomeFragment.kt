package com.github.jonasjesusamerico.we_sync_app.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.jonasjesusamerico.we_sync_app.databinding.FragmentHomeBinding

/** Placeholder authenticated destination; real content comes with the next features. */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentHomeBinding.inflate(inflater, container, false).also { _binding = it }.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
