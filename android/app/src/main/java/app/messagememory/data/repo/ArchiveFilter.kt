package app.messagememory.data.repo

enum class ArchiveFilter(val label: String) {
    ALL("All"),
    MESSAGES("Messages"),
    IMAGES("Images"),
    VIDEOS("Videos"),
    AUDIO("Audio"),
    DOCUMENTS("Documents"),
    VIEW_ONCE("View Once"),
    SAVED("Saved"),
}
