package com.walley.app.feature.assets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AssetRepository
import com.walley.app.domain.model.Asset
import com.walley.app.domain.model.Currency
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AssetsViewModel @Inject constructor(
    private val repository: AssetRepository
) : ViewModel() {

    val assets: StateFlow<List<Asset>> = repository.observeAssets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addAsset(
        name: String,
        currency: Currency,
        purchaseValue: BigDecimal,
        currentValue: BigDecimal,
        purchaseDate: LocalDate
    ) {
        viewModelScope.launch { repository.addAsset(name, currency, purchaseValue, currentValue, purchaseDate) }
    }

    fun updateCurrentValue(assetId: Long, currentValue: BigDecimal) {
        viewModelScope.launch { repository.updateCurrentValue(assetId, currentValue) }
    }

    fun deleteAsset(assetId: Long) {
        viewModelScope.launch { repository.deleteAsset(assetId) }
    }
}
