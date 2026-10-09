package it.manu.fuoriorario.core.viewmodel

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** No threads to block on the web. */
internal actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
