package com.github.njuro.medicquiz.entities

import com.fasterxml.jackson.annotation.JsonCreator

data class Answer @JsonCreator constructor(
    val letter: Char,
    val text: String
) {
    var marked: Boolean = false
    var correct: Boolean = false
}