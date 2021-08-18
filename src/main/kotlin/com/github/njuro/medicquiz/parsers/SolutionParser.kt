package com.github.njuro.medicquiz.parsers

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.njuro.medicquiz.Config.Companion.resourcesPath
import com.github.njuro.medicquiz.TestService
import com.github.njuro.medicquiz.entities.Solution
import com.github.njuro.medicquiz.entities.Subject
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files

@Component
class SolutionParser(val testService: TestService, val mapper: ObjectMapper) {
    companion object {
        private val possibleAnswers = setOf('a', 'b', 'c', 'd')
    }

    fun parseSolutionSheets() {
        val solutions = mutableSetOf<Solution>()
        val workbooks = Files.list(resourcesPath.resolve("solutionSheets")).filter {
            it.fileName.toString().endsWith(".xlsx")
        }
        for (workbook in workbooks) {
            val subject = if (workbook.fileName.toString().startsWith("bio")) Subject.BIOLOGY else Subject.CHEMISTRY
            println("Parsing solution sheet ${workbook.fileName}")
            solutions.addAll(parseSolutionSheet(subject, workbook.toFile()))
        }

        validateSolutions(solutions)
        testService.saveSolutions(solutions)
    }

    private fun parseSolutionSheet(subject: Subject, workbook: File): Set<Solution> {
        val sheet = WorkbookFactory.create(workbook).getSheetAt(0)
        val solutions = mutableSetOf<Solution>()
        sheet.forEach { row ->
            row.forEach { cell ->
                if (cell.cellType == CellType.NUMERIC) {
                    val questionNumber = cell.numericCellValue.toInt()
                    val answers =
                        row.getCell(cell.columnIndex + 1).stringCellValue.filter { possibleAnswers.contains(it) }
                            .toSet()
                    solutions.add(Solution(questionNumber, subject, answers))
                }
            }
        }

        return solutions
    }

    private fun validateSolutions(solutions: Set<Solution>) {
        solutions.forEach {
            if (it.correctAnswers.isEmpty() || it.correctAnswers.size > 4) {
                println("WARNING: Question ${it.questionNumber} from subject ${it.subject} has ${it.correctAnswers.size} answers (expected 1-4)")
            }
        }
    }
}