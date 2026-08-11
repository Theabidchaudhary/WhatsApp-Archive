package app.messagememory.data.db

import androidx.room.TypeConverter
import app.messagememory.data.db.entity.CaptureStatus
import app.messagememory.data.db.entity.MediaType
import app.messagememory.data.db.entity.MessageType
import app.messagememory.data.db.entity.PlaceholderReason

class Converters {
    @TypeConverter
    fun fromMessageType(value: MessageType): String = value.name

    @TypeConverter
    fun toMessageType(value: String): MessageType = MessageType.valueOf(value)

    @TypeConverter
    fun fromMediaType(value: MediaType): String = value.name

    @TypeConverter
    fun toMediaType(value: String): MediaType = MediaType.valueOf(value)

    @TypeConverter
    fun fromCaptureStatus(value: CaptureStatus): String = value.name

    @TypeConverter
    fun toCaptureStatus(value: String): CaptureStatus = CaptureStatus.valueOf(value)

    @TypeConverter
    fun fromPlaceholderReason(value: PlaceholderReason): String = value.name

    @TypeConverter
    fun toPlaceholderReason(value: String): PlaceholderReason = PlaceholderReason.valueOf(value)
}
