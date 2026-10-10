import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlin.jvm.JvmStatic
import data.bibleIQ.BibleIQDataModel


object NapierLogger {
    @JvmStatic
    internal fun initIosNapierLogger() {
        if (!BibleIQDataModel.RELEASE_BUILD) Napier.base(DebugAntilog())
    }
}
