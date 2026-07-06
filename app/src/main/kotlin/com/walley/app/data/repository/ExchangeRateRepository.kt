package com.walley.app.data.repository

import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import kotlinx.coroutines.flow.Flow

interface ExchangeRateRepository {
    /**
     * Emits cached rates first (when present), then fresh rates from the network.
     * Emits null only when no rates are available at all (no cache, network failed).
     */
    fun observeRates(base: Currency): Flow<ExchangeRates?>
}
