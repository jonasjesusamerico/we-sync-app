package com.github.jonasjesusamerico.we_sync_app.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import com.github.jonasjesusamerico.we_sync_app.R
import com.github.jonasjesusamerico.we_sync_app.core.ui.appContainer
import com.github.jonasjesusamerico.we_sync_app.core.ui.collectWhileStarted
import com.github.jonasjesusamerico.we_sync_app.core.ui.navigateFrom
import com.github.jonasjesusamerico.we_sync_app.databinding.FragmentHomeBinding
import com.github.jonasjesusamerico.we_sync_app.databinding.ItemCategoryTotalBinding
import com.github.jonasjesusamerico.we_sync_app.databinding.ItemRecentPurchaseBinding
import com.github.jonasjesusamerico.we_sync_app.purchase.MoneyInput
import com.github.jonasjesusamerico.we_sync_app.purchase.MonthSummary
import com.github.jonasjesusamerico.we_sync_app.purchase.PurchaseStatus
import com.google.android.material.snackbar.Snackbar
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Dashboard of the selected month (spec 003) with the "+" create menu (spec 002). */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels {
        viewModelFactory { initializer { DashboardViewModel(appContainer().purchaseRepository) } }
    }

    private val monthFormat = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"))
    private val dayFormat = DateTimeFormatter.ofPattern("dd/MM")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.fabCreate.setOnClickListener { navigateFrom(R.id.homeFragment, R.id.action_home_to_createMenu) }
        binding.buttonPreviousMonth.setOnClickListener { viewModel.previousMonth() }
        binding.buttonNextMonth.setOnClickListener { viewModel.nextMonth() }
        binding.buttonRetry.setOnClickListener { viewModel.reload() }
        observePurchaseSaved()

        collectWhileStarted(viewModel.state, ::render)
    }

    private fun render(state: DashboardUiState) {
        binding.textMonth.text = state.month.format(monthFormat)
        binding.progress.isVisible = state.isLoading
        binding.groupError.isVisible = state.error != null
        binding.textError.setText(
            if (state.error == DashboardError.NETWORK) R.string.error_network else R.string.error_unexpected,
        )

        val summary = state.summary
        binding.groupEmpty.isVisible = summary?.isEmpty == true
        binding.groupContent.isVisible = summary != null && !summary.isEmpty
        if (summary != null && !summary.isEmpty) renderSummary(summary)
    }

    private fun renderSummary(summary: MonthSummary) {
        binding.textTotal.text = MoneyInput.format(summary.total)
        binding.textPaid.text = MoneyInput.format(summary.paid)
        binding.textPending.text = MoneyInput.format(summary.pending)

        val inflater = layoutInflater
        binding.listCategories.removeAllViews()
        summary.categories.forEach { category ->
            val row = ItemCategoryTotalBinding.inflate(inflater, binding.listCategories, true)
            row.textName.text = category.name ?: getString(R.string.dashboard_other_categories)
            row.textAmount.text = MoneyInput.format(category.amount)
            row.bar.progress = (category.fraction * BAR_MAX).toInt()
        }

        binding.listLatest.removeAllViews()
        summary.latest.forEach { purchase ->
            val row = ItemRecentPurchaseBinding.inflate(inflater, binding.listLatest, true)
            row.textDate.text = purchase.date.format(dayFormat)
            row.textDescription.text = purchase.description
            row.textCategory.text = purchase.category.name
            row.textAmount.text = MoneyInput.format(purchase.amount)
            row.textPending.isVisible = purchase.status == PurchaseStatus.PENDING
        }
    }

    // Set by PurchaseFormFragment on this entry's SavedStateHandle before it pops back here.
    private fun observePurchaseSaved() {
        val handle = findNavController().getBackStackEntry(R.id.homeFragment).savedStateHandle
        collectWhileStarted(handle.getStateFlow(KEY_PURCHASE_SAVED, false)) { saved ->
            if (!saved) return@collectWhileStarted
            handle[KEY_PURCHASE_SAVED] = false
            viewModel.reload()
            Snackbar.make(binding.root, R.string.message_purchase_saved, Snackbar.LENGTH_SHORT)
                .setAnchorView(binding.fabCreate)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val KEY_PURCHASE_SAVED = "purchase_saved"
        private const val BAR_MAX = 100
    }
}
