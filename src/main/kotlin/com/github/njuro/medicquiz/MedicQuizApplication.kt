package com.github.njuro.medicquiz

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MedicQuizApplication

fun main(args: Array<String>) {
    runApplication<MedicQuizApplication>(*args)
}
