package com.github.njuro.medicquiz

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.njuro.medicquiz.Config.Companion.questionsPath
import com.github.njuro.medicquiz.Config.Companion.solutionsPath
import com.github.njuro.medicquiz.Config.Companion.testsPath
import com.github.njuro.medicquiz.entities.Answer
import com.github.njuro.medicquiz.entities.Question
import com.github.njuro.medicquiz.entities.Solution
import com.github.njuro.medicquiz.entities.Test
import com.github.njuro.medicquiz.entities.TestResult
import org.springframework.stereotype.Service

@Service
class TestService(val mapper: ObjectMapper) {

    val testsDatabase = loadTests()
    val questionsDatabase = loadQuestions()
    val solutionsDatabase = loadSolutions()

    fun saveQuestions(questions: Collection<Question>) {
        mapper.writeValue(questionsPath.toFile(), questions.sortedBy(Question::number))
    }

    fun saveSolutions(solutions: Collection<Solution>) {
        mapper.writeValue(solutionsPath.toFile(), solutions.sortedBy(Solution::questionNumber))
    }

    fun saveTests(tests: Collection<Test>) {
        mapper.writeValue(testsPath.toFile(), tests.sortedBy(Test::number))
    }

    fun getTestByNumber(testNumber: Int): Test {
        return testsDatabase.find { it.number == testNumber }
            ?: throw IllegalArgumentException("Unknown test $testNumber")
    }

    fun getQuestionsForTest(test: Test): List<Question> {
        return test.questions.flatMap { (subject, numbers) ->
            numbers.map { number ->
                questionsDatabase.find {
                    it.number == number
                        && it.subject == subject
                } ?: throw IllegalArgumentException("Unknown test question: $number from $subject")
            }
        }.sortedBy(Question::number)
    }

    fun scoreTest(test: Test, studentSolutions: List<Solution>): TestResult {
        var score = 0
        var maxScore = 0
        val questions = getQuestionsForTest(test)
        questions.forEach { question ->
            val studentSolution =
                studentSolutions.find { it.subject == question.subject && it.questionNumber == question.number }
                    ?: Solution(
                        question.number,
                        question.subject
                    )
            val correctSolution = solutionsDatabase.find { it == studentSolution }
                ?: throw IllegalArgumentException("Solution references unknown question: ${studentSolution.questionNumber} from ${studentSolution.subject}")
            for (answer in question.answers) {
                val isMarked = studentSolution.correctAnswers.contains(answer.letter)
                val isCorrect = correctSolution.correctAnswers.contains(answer.letter)
                answer.apply { marked = isMarked; correct = isCorrect }
                maxScore += if (isCorrect) 1 else 0
                score += if (isMarked) {
                    if (isCorrect) 1 else -2
                } else 0
            }
        }

        return TestResult((if (score > 0) score else 0), maxScore, questions)
    }
    
    private final fun loadQuestions(): List<Question> {
        return try {
            mapper.readValue(questionsPath.toFile(), object : TypeReference<List<Question>>() {})
                .sortedBy(Question::number).also {
                    it.forEach { question ->
                        if (question.answers.isEmpty()) {
                            question.answers.addAll(
                                listOf(
                                    Answer('a', "A"),
                                    Answer('b', "B"),
                                    Answer('c', "C"),
                                    Answer('d', "D")
                                )
                            )
                        }
                        question.answers.sortBy(Answer::letter)
                    }
                }
        } catch (ex: Exception) {
            println("Failed to load questions from file: ${ex.message}")
            listOf()
        }
    }

    private final fun loadSolutions(): List<Solution> {
        return try {
            mapper.readValue(solutionsPath.toFile(), object : TypeReference<List<Solution>>() {})
                .sortedBy(Solution::questionNumber)
        } catch (ex: Exception) {
            println("Failed to load solutions from file: ${ex.message}")
            listOf()
        }
    }

    private final fun loadTests(): List<Test> {
        return try {
            mapper.readValue(testsPath.toFile(), object : TypeReference<List<Test>>() {}).sortedBy(Test::number)
        } catch (ex: Exception) {
            println("Failed to load tests from file: ${ex.message}")
            listOf()
        }
    }
}