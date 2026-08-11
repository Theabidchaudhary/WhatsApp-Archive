package app.messagememory.ui.navigation

object Destinations {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val STORAGE_MANAGEMENT = "settings/storage"
    const val DIAGNOSTICS = "settings/diagnostics"
    const val SAMSUNG_GUIDANCE = "settings/samsung"

    const val CONVERSATION = "conversation/{conversationId}"
    fun conversation(id: Long) = "conversation/$id"

    const val MEDIA_VIEWER = "media/{messageId}"
    fun mediaViewer(messageId: Long) = "media/$messageId"
}
