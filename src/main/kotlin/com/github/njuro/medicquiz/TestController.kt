package com.github.njuro.medicquiz

import com.github.njuro.medicquiz.entities.Solution
import com.github.njuro.medicquiz.entities.Subject
import com.github.njuro.medicquiz.entities.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import javax.servlet.http.HttpServletRequest

@Controller
class TestController(val testService: TestService, val accessTokenService: AccessTokenService) {

    @Value("\${app.url}")
    lateinit var appUrl: String

    @Value("\${app.test.duration}")
    var maxDuration: Int = 120

    @GetMapping("/")
    fun pickTest(model: Model): String {
        model.addAttribute("tests", testService.testsDatabase)
        return "index"
    }

    @GetMapping("/test/{testNumber}")
    fun getTest(
        @PathVariable(name = "testNumber") testNumber: Int,
        @RequestParam(name = "token", required = false) token: String?,
        model: Model
    ): String {
        val authenticated = isAuthenticated()
        val validToken = accessTokenService.isValidAccessToken(token)
        val test = testService.getTestByNumber(testNumber)

        model.addAttribute("allowed", authenticated || validToken)
        model.addAttribute("appUrl", appUrl)
        model.addAttribute("token", token)
        model.addAttribute("test", test)
        model.addAttribute("questions", testService.getQuestionsForTest(test).withIndex())
        model.addAttribute("maxDuration", maxDuration)

        if (!authenticated) {
            accessTokenService.invalidateAccessToken(token)

            if (validToken) {
                accessTokenService.startSession(token!!)
            }
        }

        return "test"
    }

    @GetMapping("/test/{testNumber}/score")
    fun scoreTests(
        @PathVariable(name = "testNumber") testNumber: Int
    ): String {
        return "redirect:/test/${testNumber}"
    }

    @PostMapping("/test/{testNumber}/score")
    fun scoreTests(
        @PathVariable(name = "testNumber") testNumber: Int,
        @RequestParam(name = "token", required = false) token: String?,
        model: Model,
        request: HttpServletRequest
    ): String {
        val authenticated = isAuthenticated()
        val activeSession = accessTokenService.isActiveSession(token)

        val userSolutions = request.parameterMap.filter { it.key.contains("-") }.map { (question, answers) ->
            val (number, subject) = question.split("-")
            Solution(number.toInt(), Subject.valueOf(subject), answers.map { it.first() }.toSet())
        }
        val testResult = testService.scoreTest(testService.getTestByNumber(testNumber), userSolutions)

        model.addAttribute("allowed", authenticated || activeSession)
        if (activeSession) {
            model.addAttribute("duration", accessTokenService.endSession(token!!))
            model.addAttribute("maxDuration", maxDuration)
        }
        model.addAttribute("testNumber", testNumber)
        model.addAttribute("score", testResult.score)
        model.addAttribute("maxScore", testResult.maxScore)
        model.addAttribute("questions", testResult.questions.withIndex())
        return "result"
    }

    @GetMapping("/generate")
    fun showAdmin(model: Model): String {
        model.addAttribute("testNumbers", testService.testsDatabase.map(Test::number))
        model.addAttribute("appUrl", appUrl)
        return "generate"
    }

    @PostMapping("/generate")
    fun generateToken(
        @RequestParam("testNumber") testNumber: Int,
        model: Model
    ): String {
        val token = accessTokenService.generateAccessToken(testNumber)
        model.addAttribute("testNumbers", testService.testsDatabase.map(Test::number))
        model.addAttribute("appUrl", appUrl)
        model.addAttribute("token", token)
        model.addAttribute("testNumber", testNumber)
        return "generate"
    }

    private fun isAuthenticated(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication != null && authentication.principal != "anonymousUser"
    }
}