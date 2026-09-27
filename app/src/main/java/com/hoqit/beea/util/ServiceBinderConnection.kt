package com.hoqit.beea.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ServiceBinderConnection<T>(
    private val context: Context,
    private val serviceClass: Class<out Any>,
    private val onConnected: (T) -> Unit = {},
    private val onDisconnected: () -> Unit = {}
) {
    private enum class State { NONE, BINDING, CONNECTED, DISCONNECTED }

    private val _service = MutableStateFlow<T?>(null)
    val service: StateFlow<T?> = _service.asStateFlow()

    private var state = State.NONE

    private val connection = object : ServiceConnection {
        @Suppress("UNCHECKED_CAST")
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            state = State.CONNECTED
            val instance = (binder as? ServiceBinder<T>)?.getService() ?: return
            _service.value = instance
            onConnected(instance)
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            state = State.DISCONNECTED
            _service.value = null
            onDisconnected()
        }
    }

    fun bind() {
        if (state == State.BINDING || state == State.CONNECTED) return
        if (state == State.DISCONNECTED) unbind()
        context.bindService(Intent(context, serviceClass), connection, Context.BIND_AUTO_CREATE)
        state = State.BINDING
    }

    fun unbind() {
        if (state == State.NONE) return
        try {
            context.unbindService(connection)
        } catch (_: IllegalArgumentException) {
            // already torn down — safe to ignore
        }
        state = State.NONE
        _service.value = null
    }
}