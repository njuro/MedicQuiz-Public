package com.github.njuro.medicquiz

import com.github.njuro.medicquiz.parsers.SolutionParser
import com.github.njuro.medicquiz.parsers.TestParser
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ParserExecutions {

    @Autowired
    lateinit var testParser: TestParser

    @Autowired
    lateinit var solutionParser: SolutionParser

    @Test
    fun testParser() {
        testParser.parseTests()
    }

    @Test
    fun solutionParser() {
        solutionParser.parseSolutionSheets()
    }
}
