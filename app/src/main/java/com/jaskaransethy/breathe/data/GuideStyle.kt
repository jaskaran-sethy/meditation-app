package com.jaskaransethy.breathe.data

/** The breathing visual shown during a session. The orb is the design's original guide. */
enum class GuideStyle(val id: String) {
    Orb("orb"),
    /** Water that rises in a round vessel on the inhale and ebbs on the exhale. */
    Tide("tide"),
    /** A point of light tracing the breath around a rounded square, one stretch per phase. */
    Trace("trace");

    companion object {
        val Default = Orb

        fun fromId(id: String?): GuideStyle = entries.firstOrNull { it.id == id } ?: Default
    }
}
