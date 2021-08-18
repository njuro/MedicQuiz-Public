package com.github.njuro.medicquiz.entities

import com.fasterxml.jackson.annotation.JsonCreator

data class Solution @JsonCreator constructor(
    val questionNumber: Int,
    val subject: Subject,
    val correctAnswers: Set<Char> = mutableSetOf()
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Solution

        if (questionNumber != other.questionNumber) return false
        if (subject != other.subject) return false

        return true
    }

    override fun hashCode(): Int {
        var result = questionNumber
        result = 31 * result + subject.hashCode()
        return result
    }
}