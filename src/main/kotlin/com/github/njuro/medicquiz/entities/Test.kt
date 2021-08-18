package com.github.njuro.medicquiz.entities

import com.fasterxml.jackson.annotation.JsonCreator

data class Test @JsonCreator constructor(val number: Int, val questions: Map<Subject, Set<Int>> = mutableMapOf())