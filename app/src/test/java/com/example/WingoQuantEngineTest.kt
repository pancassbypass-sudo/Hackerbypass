package com.example

import com.example.data.remote.WingoRemoteRepository
import com.example.domain.WingoQuantEngine
import com.example.domain.WingoSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WingoQuantEngineTest {

    @Test
    fun `streak of 5 large draws applies mean reversion toward small`() {
        val draws = listOf(8, 7, 9, 6, 5, 2, 1, 3, 4, 0, 2, 1)
        val result = WingoQuantEngine.analyze(draws)
        assertNotNull(result)
        assertEquals(WingoSize.LARGE, result!!.currentStreakSize)
        assertEquals(5, result.currentStreakLength)
        assertEquals(WingoSize.SMALL, result.predictedSize)
        assertTrue(result.smallProbabilityPct > result.largeProbabilityPct)
    }

    @Test
    fun `parses comma separated digits and 55five json feed`() {
        val repo = WingoRemoteRepository()
        val csvDraws = repo.parsePastedWebData("3, 8, 1, 4, 9")
        assertEquals(5, csvDraws.size)
        assertEquals(3, csvDraws[0].number)
        assertEquals(WingoSize.SMALL, csvDraws[0].size)
        assertEquals(WingoSize.LARGE, csvDraws[1].size)

        val jsonSample = """
            {"code":0,"data":{"list":[{"issueNumber":"20261116001","number":"7","colour":"green"},{"issueNumber":"20261116000","number":"2","colour":"red"}]}}
        """.trimIndent()
        val jsonDraws = repo.parsePastedWebData(jsonSample)
        assertEquals(2, jsonDraws.size)
        assertEquals("20261116001", jsonDraws[0].issueNumber)
        assertEquals(7, jsonDraws[0].number)
    }
}
