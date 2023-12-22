package ind.arming.gostructtojson

import com.goide.psi.GoFile
import com.goide.psi.GoStructType
import com.goide.psi.GoTypeDeclaration
import com.goide.psi.GoTypeSpec
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil
import ind.arming.gostructtojson.GoStruct2JsonNotifier.notifyError
import ind.arming.gostructtojson.GoStruct2JsonNotifier.notifyInfo
import ind.arming.gostructtojson.GoStruct2JsonNotifier.notifyWarning
import ind.arming.gostructtojson.Utils.convertGoStructToJson
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import java.util.*


class GoStruct2JsonAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val editor: Editor? = e.dataContext.getData(CommonDataKeys.EDITOR)
        val project: Project? = editor?.project
        val document: Document? = editor?.document

        val extension: String = Objects.requireNonNull(FileDocumentManager.getInstance().getFile(document!!))?.extension.toString()
        if (extension.toLowerCase() != "go") {
            return
        }
        val psiFile = e.dataContext.getData(CommonDataKeys.PSI_FILE) as GoFile?
        val offset: Int = editor.caretModel.offset

        val cursorOffset = psiFile!!.findElementAt(offset)
        val goTypeDeclarationContext = PsiTreeUtil.getContextOfType(cursorOffset, GoTypeDeclaration::class.java)

        if (goTypeDeclarationContext == null) {
            notifyWarning(project,
                    "can't get go struct,please put the cursor on a struct then right click 'Convert Struct To JSON' label")
            return
        }

        val goTypeSpec = PsiTreeUtil.getChildOfType(goTypeDeclarationContext, GoTypeSpec::class.java)

        if (goTypeSpec == null) {
            notifyWarning(project,
                    "can't get go struct,please put the cursor on a struct then right click 'Convert Struct To JSON' label")
            return
        } else {
            val goSpecType = goTypeSpec.specType

            val goStructType = PsiTreeUtil.getChildOfType(goSpecType, GoStructType::class.java)
            if (goStructType == null) {
                notifyWarning(project,
                        "can't get go struct,please put the cursor on a struct then right click 'Convert Struct To JSON' label")
                return
            }
            val goStructName = goTypeSpec.identifier.text

            val result = convertGoStructToJson(goStructType)
            if (result !== "error") {
                val selection = StringSelection(result)
                val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(selection, selection)
                val msg = "Convert Struct $goStructName to json success,copied to clipboard."
                notifyInfo(project, """
     $msg
     $result
     """.trimIndent())
            } else {
                val err = "Convert Struct $goStructName failed,please put the cursor on a struct then right click 'Convert Struct To JSON' label."
                notifyError(project, err)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val project: Project? = e.project
        e.presentation.isEnabledAndVisible = project != null
    }
}