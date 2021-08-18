package com.github.njuro.medicquiz.parsers

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.github.njuro.medicquiz.Config.Companion.imageMappingsPath
import com.github.njuro.medicquiz.Config.Companion.resourcesPath
import com.github.njuro.medicquiz.Config.Companion.subjectMappingsPath
import com.github.njuro.medicquiz.TestService
import com.github.njuro.medicquiz.entities.Answer
import com.github.njuro.medicquiz.entities.Question
import com.github.njuro.medicquiz.entities.Subject
import com.github.njuro.medicquiz.entities.Test
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.springframework.stereotype.Component
import org.zwobble.mammoth.DocumentConverter
import java.awt.Desktop
import java.io.ByteArrayInputStream
import java.io.File
import java.math.BigInteger
import java.nio.file.Files
import java.security.MessageDigest
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.streams.toList

@Component
class TestParser(val testService: TestService, val mapper: ObjectMapper) {
    companion object {
        private val converter = DocumentConverter()
        private val testNameRegex = Regex("TC(\\d+)\\.doc(?:x)?")
        private val questionNumberRegex = Regex("(\\d+)\\s*(?:\\.)?\\s*(.+)")
        private val strongTagRegex = Regex("</?strong>")
        private val imgTagRegex = Regex("<img.+>")
    }

    private val questionDatabase = mutableSetOf<Question>()
    private val testDatabase = mutableSetOf<Test>()

    private val imageMappings =
        try {
            mapper.readValue(
                imageMappingsPath.toFile(),
                object : TypeReference<MutableMap<Int, MutableList<String>>>() {}
            )
        } catch (ex: MismatchedInputException) {
            mutableMapOf<Int, MutableList<String>>()
        }

    private val subjectMappings =
        try {
            mapper.readValue(
                subjectMappingsPath.toFile(),
                object : TypeReference<MutableMap<Int, Int>>() {}
            )
        } catch (ex: MismatchedInputException) {
            mutableMapOf<Int, Int>()
        }

    fun parseTests() {
        questionDatabase.clear()
        testDatabase.clear()

        val testFiles = Files.list(resourcesPath.resolve("tests")).filter {
            it.fileName.toString().endsWith(".docx")
        }
            .toList()
            .associateBy { testNameRegex.matchEntire(it.fileName.toString())!!.groupValues[1].toInt() }
            .toSortedMap()

        for ((testNumber, testFile) in testFiles) {
            println("=========== Parsing test $testNumber")
            val questions =
                parseTest(testFile.toFile(), subjectMappings.getOrDefault(testNumber, -1)).groupBy(Question::subject)
                    .mapValues { it.value.map(Question::number).toSet() }
            testDatabase.add(Test(testNumber, questions))
        }

        validateQuestions()
        validateTests()

        testService.saveQuestions(questionDatabase)
        testService.saveTests(testDatabase)
    }

    private fun parseTest(testFile: File, switchAtQuestion: Int): Set<Question> {
        val result = converter.convertToHtml(testFile)
        result.warnings.forEach { println("WARNING: $it") }
        return parseQuestions(Jsoup.parse(result.value).selectFirst("body"), switchAtQuestion)
    }

    private fun parseQuestions(document: Element, switchAtQuestion: Int): Set<Question> {
        fun String.replaceSpaces(): String {
            return replace("&nbsp;", " ")
        }

        val questions = mutableSetOf<Question>()
        var switchSubject = false

        fun addQuestion(questionNumber: Int, body: String, type: Question.Type, answers: Set<Answer> = setOf()) {
            val subject = if (switchSubject) Subject.BIOLOGY else Subject.CHEMISTRY
            val parsedQuestion = Question(questionNumber, subject, body, type, answers.toMutableList())
            questions.add(parsedQuestion)
            questionDatabase.add(parsedQuestion)
        }

        fun getImageTag(questionNumber: Int): String {
            return """<p><img src="/static/$questionNumber.jpg"></p>"""
        }

        fun parseQuestion(question: Element) {
            val questionText = question.text()
            if (questionText.isBlank()) {
                question.children().filter { it.tagName() == "img" }.forEach {
                    val questionNumber = getQuestionNumberFromImage(it.attr("src"))
                    println("$questionNumber. IMAGE QUESTION")
                    addQuestion(
                        questionNumber,
                        "Otázka číslo $questionNumber na obrázku:" + getImageTag(questionNumber),
                        Question.Type.IMAGE
                    )
                }
                return
            }

            if (!questionText.trim()[0].isDigit()) {
                return
            }

            val questionHeader = question.html().replace(strongTagRegex, "").replace(imgTagRegex, "").replaceSpaces()
            val (questionNumber, body) = questionNumberRegex.matchEntire(questionHeader)!!.destructured
            println("$questionNumber. $body")
            if (questionNumber.toInt() == switchAtQuestion && questions.size > 1) {
                switchSubject = true
            }
            val nextElement = question.nextElementSibling()
            val answers: Set<Answer> = when (nextElement.tagName()) {
                "ol" -> {
                    nextElement.select("li")
                        .mapIndexed { index, answer ->
                            Answer(
                                'a' + index,
                                answer.html().trim().replaceSpaces()
                            )
                        }
                        .toSet()
                }
                "p" -> {
                    if (nextElement.childrenSize() > 0 && nextElement.child(0).tagName() == "img") {
                        addQuestion(
                            questionNumber.toInt(),
                            body + getImageTag(questionNumber.toInt()),
                            Question.Type.MIXED
                        )
                        return
                    }

                    (0 until 4).map {
                        Answer(
                            'a' + it,
                            question.nextElementSiblings()[it].html().split(")")[1].trim().replaceSpaces()
                        )
                    }.toSet()
                }
                else -> throw IllegalArgumentException("Unknown structure ${nextElement.tagName()}")
            }

            addQuestion(questionNumber.toInt(), body, Question.Type.TEXT, answers)
        }

        document.children().filter { it.tagName() == "p" }.forEach(::parseQuestion)

        return questions
    }

    private fun getQuestionNumberFromImage(imageData: String): Int {
        val base64Image: String = imageData.split(",")[1]
        val imageBytes = Base64.getDecoder().decode(base64Image)
        val filename =
            BigInteger(1, MessageDigest.getInstance("MD5").digest(base64Image.toByteArray())).toString(16)
        val imageFile =
            resourcesPath.resolve("images").resolve("$filename.jpg").toFile()

        if (!imageFile.exists()) {
            val image = ImageIO.read(ByteArrayInputStream(imageBytes))
            ImageIO.write(image, "jpg", imageFile)
        }

        return imageMappings.filterValues { it.contains(filename) }.keys.firstOrNull() ?: run {
            println("Missing question number for image $filename: ")
            Desktop.getDesktop().open(imageFile)
            val newNumber = readLine()!!.toInt()
            val newList = imageMappings.getOrDefault(newNumber, mutableListOf()) + mutableListOf(filename)
            imageMappings[newNumber] = newList.toMutableList()
            mapper.writeValue(imageMappingsPath.toFile(), imageMappings.toSortedMap())
            newNumber
        }
    }

    private fun validateTests() {
        testDatabase.forEach {
            it.questions.forEach { (subject, questions) ->
                if (questions.size != 80) {
                    println("WARNING: Test ${it.number} has ${questions.size} questions from subject $subject (expected 80)")
                }
            }
        }
    }

    private fun validateQuestions() {
        questionDatabase.forEach {
            val expected = when (it.type) {
                Question.Type.TEXT -> 4
                Question.Type.IMAGE -> 0
                Question.Type.MIXED -> 0
            }
            val actual = it.answers.size
            if (expected != actual) {
                println("WARNING: Question ${it.number} of subject ${it.subject} has $actual answers (expected $expected)")
            }
        }

        Subject.values().forEach { subject ->
            (1..questionDatabase.filter { it.subject == subject }.maxBy(Question::number)!!.number).forEach { number ->
                if (questionDatabase.find { it.number == number && it.subject == subject } == null) {
                    println("WARNING: Missing question $number of subject $subject")
                }
            }
        }
    }
}
