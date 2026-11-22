import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlin.jvm.JvmStatic
import data.bible.BibleState


object NapierLogger {
    @JvmStatic
    internal fun initIosNapierLogger() {
        if (!BibleState.RELEASE_BUILD) Napier.base(DebugAntilog())
    }
}
