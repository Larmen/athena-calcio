package se.anders.athenacalcio

import org.junit.Assert.assertTrue
import org.junit.Test
import se.anders.athenacalcio.data.licences.Licences
import java.io.File

class LicencesTest {
    @Test fun everyListedLicenceFileIsShipped() {
        val dir = File("src/main/assets/licenses")
        Licences.all.forEach { assertTrue("${it.assetFile} missing", File(dir, it.assetFile).isFile) }
    }
}
