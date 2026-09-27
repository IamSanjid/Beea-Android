package com.hoqit.beea.util

interface ServiceBinder<T> {
    fun getService(): T
}