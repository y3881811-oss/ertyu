package com.ertyu

import com.ertyu.model.CryptoState
import kotlinx.coroutines.flow.MutableStateFlow

object CryptoStateHolder {
    val state = MutableStateFlow(CryptoState())
}