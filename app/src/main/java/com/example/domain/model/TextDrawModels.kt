package com.example.domain.model

import com.example.core.AppConstants

enum class TdFont(val value: Int) {
    FONT_0(0),
    FONT_1(1),
    FONT_2(2),
    FONT_3(3),
    SPRITE_4(4),
    MODEL_PREVIEW_5(5);

    companion object {
        fun fromInt(value: Int): TdFont = entries.firstOrNull { it.value == value } ?: FONT_1
    }
}

enum class TdAlignment(val value: Int) {
    LEFT(1),
    CENTER(2),
    RIGHT(3);

    companion object {
        fun fromInt(value: Int): TdAlignment = entries.firstOrNull { it.value == value } ?: LEFT
    }
}

enum class CanvasAspectRatio(val ratioFloat: Float) {
    RATIO_4_3(4f / 3f),
    RATIO_16_9(16f / 9f);

    companion object {
        fun fromStorageKey(key: String): CanvasAspectRatio =
            entries.firstOrNull { it.name == key } ?: RATIO_16_9
    }
}

enum class PawnSyntaxMode {
    SAMP_CLASSIC,
    OPEN_MP;

    companion object {
        fun fromStorageKey(key: String): PawnSyntaxMode =
            entries.firstOrNull { it.name == key } ?: SAMP_CLASSIC
    }
}

data class TextDrawElement(
    val id: Long,
    val varName: String,
    val isPlayerTextDraw: Boolean = false,
    val x: Float = 320f,
    val y: Float = 240f,
    val text: String = "TextDraw",
    val font: TdFont = TdFont.FONT_1,
    val alignment: TdAlignment = TdAlignment.LEFT,
    val letterSizeX: Float = 0.35f,
    val letterSizeY: Float = 1.4f,
    val textSizeX: Float = 450f,
    val textSizeY: Float = 20f,
    val colorRgba: Long = AppConstants.DEFAULT_COLOR_RGBA,
    val boxColorRgba: Long = AppConstants.DEFAULT_BOX_COLOR_RGBA,
    val backgroundColorRgba: Long = AppConstants.DEFAULT_BG_COLOR_RGBA,
    val useBox: Boolean = false,
    val outline: Int = 1,
    val shadow: Int = 0,
    val proportional: Boolean = true,
    val selectable: Boolean = false,
    val previewModel: Int = 411,
    val previewRotX: Float = -16f,
    val previewRotY: Float = 0f,
    val previewRotZ: Float = -55f,
    val previewZoom: Float = 1f,
    val previewVehCol1: Int = -1,
    val previewVehCol2: Int = -1,
    val zOrder: Int = 0,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false
)

data class TdProject(
    val id: Long,
    val name: String,
    val aspectRatio: CanvasAspectRatio,
    val syntaxMode: PawnSyntaxMode,
    val updatedAt: Long,
    val elements: List<TextDrawElement>
)

data class ProjectSummary(
    val id: Long,
    val name: String,
    val elementCount: Int,
    val aspectRatio: CanvasAspectRatio,
    val syntaxMode: PawnSyntaxMode,
    val updatedAt: Long
)

data class SpriteIndexEntry(
    val txdName: String,
    val spriteName: String,
    val txdRelativePath: String,
    val width: Int,
    val height: Int
) {
    val sampNotation: String get() = "$txdName:$spriteName"
    val cacheKey: String get() = "${txdName.lowercase()}:${spriteName.lowercase()}"
}

sealed interface AssetPackStatus {
    data object NotLoaded : AssetPackStatus
    data object Loading : AssetPackStatus
    data class Loaded(
        val sourceLabel: String,
        val hasFontsDat: Boolean,
        val hasFontsTxd: Boolean,
        val txdCount: Int,
        val spriteCount: Int
    ) : AssetPackStatus
    data class Error(val message: String) : AssetPackStatus
}
