package com.zaneschepke.logcatter.model

enum class LogLevel(val signifier: String) {
    DEBUG("D") {
        override fun color(): Long {
            return 0xFF2196F3
        }
    },
    INFO("I") {
        override fun color(): Long {
            return 0xFF4CAF50
        }
    },
    ASSERT("A") {
        override fun color(): Long {
            return 0xFF9C27B0
        }
    },
    WARNING("W") {
        override fun color(): Long {
            return 0xFFFFC107
        }
    },
    ERROR("E") {
        override fun color(): Long {
            return 0xFFF44336
        }
    },
    VERBOSE("V") {
        override fun color(): Long {
            return 0xFF000000
        }
    };

    abstract fun color(): Long

    companion object {
        fun fromSignifier(signifier: String): LogLevel {
            return when (signifier) {
                DEBUG.signifier -> DEBUG
                INFO.signifier -> INFO
                WARNING.signifier -> WARNING
                VERBOSE.signifier -> VERBOSE
                ASSERT.signifier -> ASSERT
                ERROR.signifier -> ERROR
                else -> VERBOSE
            }
        }
    }
}
