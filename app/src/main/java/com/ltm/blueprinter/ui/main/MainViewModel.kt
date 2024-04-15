package com.ltm.blueprinter.ui.main

import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

public enum class Sources {
    CONTACTS,
    PDF
}

class MainViewModel(): ViewModel() {

    private var _printValue = MutableStateFlow<String?>(null)
    val value = _printValue.asStateFlow()

    fun importFromSource(source: Sources, values: Map<String, String>){
        when(source){
            Sources.CONTACTS -> {
                importFromContacts(values)
            }
            else -> {

            }
        }
    }

    fun importFromContacts(values: Map<String, String>) {
        var sb = StringBuilder()

        for ((key, value) in values) {
            sb.append("${key}:")
            sb.append(value)
            sb.append("\n")
        }

        _printValue.value = sb.toString()
    }

    companion object {
        val CONTACTS_PROJECTION = arrayOf(
            ContactsContract.Data._ID,
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.DISPLAY_NAME_PRIMARY,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
        )
    }
}

class MainViewModelFactory(): ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(MainViewModel::class.java)){
            return MainViewModel() as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}