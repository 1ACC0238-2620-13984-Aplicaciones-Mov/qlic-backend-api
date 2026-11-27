package com.wasd.qlic.acceptance

import io.cucumber.core.cli.Main
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Runs the Gherkin acceptance features of src/test/resources/features with Cucumber as part of the test task.
 * The HTML report is written to build/reports/cucumber/qlic-acceptance.html.
 */
class AcceptanceFeaturesTest {

    @Test
    @DisplayName("The Gherkin acceptance features pass")
    fun acceptanceFeatures() {
        val exitStatus = Main.run(
            arrayOf(
                "--glue", "com.wasd.qlic.acceptance",
                "--plugin", "pretty",
                "--plugin", "html:build/reports/cucumber/qlic-acceptance.html",
                "--monochrome",
                "classpath:features"
            ),
            Thread.currentThread().contextClassLoader
        )
        assertEquals(0, exitStatus.toInt(), "Some Gherkin scenarios failed; see the output above")
    }
}
