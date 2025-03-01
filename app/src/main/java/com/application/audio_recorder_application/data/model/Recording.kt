package com.application.audio_recorder_application.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey val id: String,
    val fileName: String,
    val filePath: String,
    val duration: Long,
    val size: Long,
    val dateCreated: Date,
    val category: String = "Общее",
    val tags: List<String> = emptyList(),
    val isEncrypted: Boolean = false,
    val transcription: String? = null,
    val waveformData: ByteArray? = null,
    val format: String = "3gp",
    val isFavorite: Boolean = false,
    val notes: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Recording

        if (id != other.id) return false
        if (fileName != other.fileName) return false
        if (filePath != other.filePath) return false
        if (duration != other.duration) return false
        if (size != other.size) return false
        if (dateCreated != other.dateCreated) return false
        if (category != other.category) return false
        if (tags != other.tags) return false
        if (isEncrypted != other.isEncrypted) return false
        if (transcription != other.transcription) return false
        if (waveformData != null) {
            if (other.waveformData == null) return false
            if (!waveformData.contentEquals(other.waveformData)) return false
        } else if (other.waveformData != null) return false
        if (format != other.format) return false
        if (isFavorite != other.isFavorite) return false
        if (notes != other.notes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + filePath.hashCode()
        result = 31 * result + duration.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + dateCreated.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + isEncrypted.hashCode()
        result = 31 * result + (transcription?.hashCode() ?: 0)
        result = 31 * result + (waveformData?.contentHashCode() ?: 0)
        result = 31 * result + format.hashCode()
        result = 31 * result + isFavorite.hashCode()
        result = 31 * result + (notes?.hashCode() ?: 0)
        return result
    }
} 