package com.github.jonasjesusamerico.we_sync_app.purchase.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import com.github.jonasjesusamerico.we_sync_app.R
import com.github.jonasjesusamerico.we_sync_app.core.ui.appContainer
import com.github.jonasjesusamerico.we_sync_app.core.ui.collectWhileStarted
import com.github.jonasjesusamerico.we_sync_app.databinding.FragmentPurchaseFormBinding
import com.github.jonasjesusamerico.we_sync_app.home.HomeFragment
import com.github.jonasjesusamerico.we_sync_app.purchase.PurchaseField
import com.github.jonasjesusamerico.we_sync_app.purchase.PurchaseStatus
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputLayout
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class PurchaseFormFragment : Fragment() {

    private var _binding: FragmentPurchaseFormBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: PurchaseFormViewModel by viewModels {
        viewModelFactory { initializer { PurchaseFormViewModel(appContainer().purchaseRepository) } }
    }

    private lateinit var moneyWatcher: MoneyTextWatcher

    // Lists currently in the dropdown adapters, to rebuild them only when they change.
    private var shownCategories: List<*>? = null
    private var shownPaymentMethods: List<*>? = null

    private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPurchaseFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        shownCategories = null
        shownPaymentMethods = null

        moneyWatcher = MoneyTextWatcher(binding.inputAmount, viewModel::onAmountChanged)
        binding.inputAmount.addTextChangedListener(moneyWatcher)
        binding.inputDescription.doAfterTextChanged { viewModel.onDescriptionChanged() }
        binding.inputDate.setOnClickListener { showDatePicker() }
        binding.inputCategory.setOnItemClickListener { _, _, position, _ ->
            viewModel.onCategorySelected(viewModel.state.value.categories[position].id)
        }
        binding.inputPaymentMethod.setOnItemClickListener { _, _, position, _ ->
            viewModel.onPaymentMethodSelected(viewModel.state.value.paymentMethods[position].id)
        }
        binding.toggleStatus.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            viewModel.onStatusSelected(if (checkedId == R.id.button_status_pending) PurchaseStatus.PENDING else PurchaseStatus.PAID)
        }
        binding.buttonSave.setOnClickListener { viewModel.submit(binding.inputDescription.text.toString()) }
        binding.buttonRetryOptions.setOnClickListener { viewModel.retryLoadOptions() }
        datePicker()?.addOnPositiveButtonClickListener(::onDatePicked) // re-attach after recreation

        collectWhileStarted(viewModel.state, ::render)
    }

    private fun render(state: PurchaseFormUiState) {
        binding.progressOptions.isVisible = state.isLoadingOptions
        binding.groupOptionsError.isVisible = state.optionsError != null
        state.optionsError?.let { binding.textOptionsError.setText(it.textRes()) }

        moneyWatcher.show(state.amount)
        binding.inputDate.setText(state.date.format(dateFormat))

        if (shownCategories !== state.categories) {
            binding.inputCategory.setAdapter(dropdownAdapter(state.categories.map { it.name }))
            shownCategories = state.categories
        }
        if (shownPaymentMethods !== state.paymentMethods) {
            binding.inputPaymentMethod.setAdapter(dropdownAdapter(state.paymentMethods.map { it.name }))
            shownPaymentMethods = state.paymentMethods
        }
        binding.inputCategory.setText(state.categories.firstOrNull { it.id == state.categoryId }?.name.orEmpty(), false)
        binding.inputPaymentMethod.setText(
            state.paymentMethods.firstOrNull { it.id == state.paymentMethodId }?.name.orEmpty(),
            false,
        )
        binding.toggleStatus.check(
            if (state.status == PurchaseStatus.PENDING) R.id.button_status_pending else R.id.button_status_paid,
        )

        binding.layoutAmount.showError(state, PurchaseField.AMOUNT)
        binding.layoutDescription.showError(state, PurchaseField.DESCRIPTION)
        binding.layoutDate.showError(state, PurchaseField.DATE)
        binding.layoutCategory.showError(state, PurchaseField.CATEGORY)
        binding.layoutPaymentMethod.showError(state, PurchaseField.PAYMENT_METHOD)

        binding.textMessage.isVisible = state.message != null
        state.message?.let { binding.textMessage.setText(it.textRes()) }

        listOf(
            binding.layoutAmount, binding.layoutDescription, binding.layoutDate,
            binding.layoutCategory, binding.layoutPaymentMethod,
        ).forEach { it.isEnabled = state.canEdit }
        binding.toggleStatus.isEnabled = state.canEdit
        binding.buttonSave.isEnabled = state.canEdit
        binding.progressSave.isVisible = state.isSaving

        if (state.isSaved) finishWithSuccess()
    }

    private fun TextInputLayout.showError(state: PurchaseFormUiState, field: PurchaseField) {
        error = state.errors[field]?.let { getString(it.textRes()) }
    }

    private fun dropdownAdapter(names: List<String>) =
        ArrayAdapter(requireContext(), R.layout.item_dropdown, names)

    private fun showDatePicker() {
        if (!viewModel.state.value.canEdit || datePicker() != null) return
        MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.date_picker_title)
            .setSelection(viewModel.state.value.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
            .build()
            .apply { addOnPositiveButtonClickListener(::onDatePicked) }
            .show(childFragmentManager, DATE_PICKER_TAG)
    }

    // MaterialDatePicker reports the selected day as UTC midnight.
    private fun onDatePicked(selection: Long) {
        viewModel.onDateSelected(Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC).toLocalDate())
    }

    @Suppress("UNCHECKED_CAST")
    private fun datePicker() = childFragmentManager.findFragmentByTag(DATE_PICKER_TAG) as? MaterialDatePicker<Long>

    private fun finishWithSuccess() {
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.purchaseFormFragment) return
        navController.previousBackStackEntry?.savedStateHandle?.set(HomeFragment.KEY_PURCHASE_SAVED, true)
        navController.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val DATE_PICKER_TAG = "purchase_date_picker"
    }
}

