package com.github.xepozz.infection.tests

import com.github.xepozz.infection.InfectionBundle
import com.github.xepozz.infection.InfectionIcons
import com.intellij.openapi.project.Project
import com.jetbrains.php.testFramework.PhpTestFrameworkFormDecorator
import com.jetbrains.php.testFramework.PhpTestFrameworkFormDecorator.PhpDownloadableTestFormDecorator
import com.jetbrains.php.testFramework.PhpTestFrameworkType
import com.jetbrains.php.testFramework.ui.PhpTestFrameworkBaseConfigurableForm
import com.jetbrains.php.testFramework.ui.PhpTestFrameworkConfigurableForm

class InfectionFrameworkType : PhpTestFrameworkType() {
    override fun getDisplayName() = InfectionBundle.message("infection.local.run.display.name")

    override fun getID() = ID

    override fun getIcon() = InfectionIcons.INFECTION

//    override fun getHelpTopic() = "reference.webide.settings.project.settings.php.infection"

    override fun getComposerPackageNames() = arrayOf("infection/infection")

    override fun getDecorator(): PhpTestFrameworkFormDecorator {
        return object : PhpDownloadableTestFormDecorator("https://github.com/infection/infection/releases") {
            override fun decorate(
                project: Project?,
                form: PhpTestFrameworkBaseConfigurableForm<*>
            ): PhpTestFrameworkConfigurableForm<*> {
                form.setVersionDetector(InfectionVersionDetector)
                return super.decorate(project, form)
            }
        }
    }

    companion object {
        val ID = "Infection"
        val INSTANCE: InfectionFrameworkType
            get() = getTestFrameworkType(ID) as InfectionFrameworkType
    }
}
