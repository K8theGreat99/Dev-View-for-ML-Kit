package com.k8thegreat.devview.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.k8thegreat.devview.data.DevViewDatabase
import com.k8thegreat.devview.data.Sample
import com.k8thegreat.devview.data.SampleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SampleRepository(
        context = application,
        dao = DevViewDatabase.get(application).sampleDao(),
    )

    val samples: StateFlow<List<Sample>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _importing = MutableStateFlow(0)
    /** How many images are still being processed, so the UI can show real progress. */
    val importing: StateFlow<Int> = _importing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _importing.value += uris.size
            // Sequential on purpose: several full-size bitmaps decoded at once is the
            // quickest way to run a mid-range phone out of memory.
            uris.forEach { uri ->
                repository.addImage(uri).onFailure { failure ->
                    _error.value = failure.message ?: "Could not read that image"
                }
                _importing.value -= 1
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun observeSample(id: String) = repository.observeById(id)

    fun prettyJson(sample: Sample): String = repository.prettyJson(sample.document)

    fun clearError() { _error.value = null }
}
