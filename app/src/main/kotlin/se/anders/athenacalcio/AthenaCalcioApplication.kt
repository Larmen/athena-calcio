package se.anders.athenacalcio

import android.app.Application
import se.anders.athenacalcio.core.AppContainer

class AthenaCalcioApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
