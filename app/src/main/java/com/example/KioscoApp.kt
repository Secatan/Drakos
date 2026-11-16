package com.example

import android.app.Application
import com.example.data.KioscoDatabase
import com.example.data.repository.CloudSyncRepository
import com.example.data.repository.KioscoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class KioscoApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { KioscoDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { KioscoRepository(database) }
    val cloudSyncRepository by lazy { CloudSyncRepository(this) }
}
