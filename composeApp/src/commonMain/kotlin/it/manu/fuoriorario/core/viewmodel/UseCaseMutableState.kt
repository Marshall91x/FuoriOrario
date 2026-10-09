package it.manu.fuoriorario.core.viewmodel

import it.manu.fuoriorario.core.error.ErrorHandler

/**
 * This class is used to handle the response with a mutable state in the ComposeViewModel
 *
 * Usage in ui:
 *
 * val uiState by viewModel.uiState.collectAsStateWithLifecycle()
 * when (state = uiState.state) {
 *  is UseCaseMutableState.Error -> ...
 *  is UseCaseMutableState.Loading -> ...
 *  is UseCaseMutableState.ShowData -> ...
 *  else -> ...
 * }
 *
 */
sealed class UseCaseMutableState<out T> {
    object Loading : UseCaseMutableState<Nothing>()

    data class ShowData<T>(val items: T) : UseCaseMutableState<T>()

    data class Error(val handler: ErrorHandler) : UseCaseMutableState<Nothing>()
}
