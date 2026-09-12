package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

class ParkerTestTemporaryContainmentTest {
    @Test
    fun `test JVM temporary storage is beneath Parker build directory`() {
        val temporaryRoot = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize()
        val buildRoot = Path.of("build").toAbsolutePath().normalize()
        assertTrue(temporaryRoot.startsWith(buildRoot), "java.io.tmpdir=$temporaryRoot must be beneath $buildRoot")

        val scratch = Files.createTempDirectory("containment-verification-")
        Files.writeString(scratch.resolve("scratch.txt"), "ephemeral")
        assertTrue(Files.exists(scratch.resolve("scratch.txt")))
    }
}
