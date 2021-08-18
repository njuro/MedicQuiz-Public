package com.github.njuro.medicquiz.entities

import com.fasterxml.jackson.annotation.JsonCreator

data class Question @JsonCreator constructor(
    val number: Int,
    val subject: Subject,
    val text: String,
    val type: Type,
    val answers: MutableList<Answer> = mutableListOf()
) {
    enum class Type { TEXT, IMAGE, MIXED }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Question

        if (number != other.number) return false
        if (subject != other.subject) return false

        return true
    }

    override fun hashCode(): Int {
        var result = number
        result = 31 * result + subject.hashCode()
        return result
    }
}