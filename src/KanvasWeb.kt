import jquery.JQuery
import org.w3c.dom.events.Event
import kotlin.browser.document
import kotlin.js.Math

class Editor(initialText: String = "", initialIndex: Int = 0) {
    private var caretIndex: Int = initialIndex
    private var nLines: Int
    private var text: String = initialText

    private fun textBeforeCaret() : String {
        return if (this.caretIndex == 0) {
            ""
        } else {
            this.text.substring(0, this.caretIndex)
        }
    }

    private fun textAfterCaret() : String {
        return if (this.caretIndex  == this.text.length) {
            ""
        } else {
            this.text.substring(this.caretIndex );
        }
    }

    private fun currentLine() : Int = this.textBeforeCaret().lines().size - 1

    fun currentIndex() : Int = this.caretIndex

    internal fun numberOfLines() : Int = this.nLines

    private fun currentColumn() : Int  {
        val i = this.textBeforeCaret().lastIndexOf("\n")
        if (i == -1) {
            return this.caretIndex;
        }
        return this.caretIndex - i - 1
    }

    fun numberOfColumnsForLine(line: Int) : Int  {
        val lines = this.text.lines()
        return lines[line].length
    }

    internal fun goTo(line: Int, column: Int) {
        println("Going to L$line C$column")
        val l = when {
            line < 0 -> 0
            line >= this.numberOfLines() -> this.numberOfLines() - 1
            else -> line
        }
        val c = when {
            column < 0 -> 0
            column >= this.numberOfColumnsForLine(l) -> this.numberOfColumnsForLine(l)
            else -> column
        }
        println("  or better going to L$l C$c")
        var newIndex = 0
        for (i in 0 until l) {
            newIndex = this.text.indexOf("\n", newIndex) + 1
        }
        newIndex += c
        println("  newIndex $newIndex")
        this.caretIndex = newIndex
    }

    private fun toHtml(text: String) : String {
        return text.replace("\n", "<br/>").replace(" ", "&nbsp;")
    }

    private fun removeLine() {
        if (this.nLines == 0) {
            return
        }
        this.nLines--
    }

    private fun addLine() {
        this.nLines++
    }

    fun generateContentHtml() : String {
        return this.toHtml(this.textBeforeCaret()) + "<span class='cursor-placeholder'>|</span>" + this.toHtml(this.textAfterCaret())
    }

    fun generateLinesHtml() : String {
        var code = ""
        for (i in 1..this.nLines) {
            code += "<span>$i</span><br/>"
        }
        return code
    }

    fun type(c: String) {
        if (c == "\n") {
            this.addLine()
        }
        this.text = this.textBeforeCaret() + c + this.textAfterCaret()
        this.caretIndex = this.caretIndex + 1
    }

    fun deletePrevChar() : Boolean {
        return if (this.textBeforeCaret().isNotEmpty()) {
            if (this.text[this.caretIndex - 1] == '\n') {
                this.removeLine()
            }
            this.text = this.textBeforeCaret().substring(0, this.textBeforeCaret().length - 1) + this.textAfterCaret()
            this.caretIndex--
            true
        } else {
            false
        }
    }

    fun deleteNextChar() : Boolean {
        return if (this.textAfterCaret().isNotEmpty()) {
            if (this.text[this.caretIndex + 1] == '\n') {
                this.removeLine()
            }
            this.text = this.textBeforeCaret() + this.textAfterCaret().drop(1)
            true
        } else {
            false
        }
    }

    fun moveLeft() : Boolean {
        return if (this.caretIndex == 0) {
            false
        } else {
            this.caretIndex--
            true
        }
    }

    fun moveRight() : Boolean {
        return if (this.caretIndex == this.text.length) {
            false
        } else {
            this.caretIndex++
            true
        }
    }

    fun moveUp() : Boolean {
        return if (this.currentLine() == 0) {
            false
        } else {
            this.goTo(this.currentLine() - 1, this.currentColumn())
            true
        }
    }

    fun moveDown() : Boolean {
        return if (this.currentLine() == (this.numberOfLines() - 1)) {
            false
        } else {
            this.goTo(this.currentLine() + 1, this.currentColumn())
            true
        }
    }

    fun goToStartOfLine() : Boolean {
        this.goTo(this.currentLine(), 0)
        return true
    }

    fun goToEndOfLine() : Boolean {
        this.goTo(this.currentLine(), this.numberOfColumnsForLine(this.currentLine()))
        return true
    }

    init {
        if (initialIndex > initialText.length) {
            throw Error("Invalid initial index")
        }
        this.nLines = this.text.lines().size
    }
}

fun updateHtml(editor: Editor) {
    js("$(\"#content\")[0]").innerHTML = editor.generateContentHtml()
    js("$(\"#lines\")[0]").innerHTML = editor.generateLinesHtml()
    val cursorPos = js("$(\".cursor-placeholder\")").position()
    var delta = js("$(\".cursor-placeholder\")").height() / 4.0
    js("$(\".blinking-cursor\").css({ top: cursorPos.top, left: cursorPos.left - delta })")
}

val KEY_ENTER = 13
val KEY_DELETE = 8
val KEY_CANC = 46
val KEY_ARROW_LEFT = 37
val KEY_ARROW_RIGHT = 39
val KEY_ARROW_UP = 38
val KEY_ARROW_DOWN = 40
val KEY_HOME = 36
val KEY_END = 35

fun main(args: Array<String>) {
    println("Starting...")
    val editor = Editor()
    updateHtml(editor)

    document.onkeypress =  { e : dynamic ->
        val c = if (e.which == KEY_ENTER) "\n" else js("String.fromCharCode(e.which)") as String
        editor.type(c)
        updateHtml(editor)
    }
    document.onkeydown = { e : dynamic ->
        val needUpdate = when (e.which) {
            KEY_CANC -> editor.deleteNextChar()
            KEY_DELETE -> editor.deletePrevChar()
            KEY_ARROW_LEFT -> editor.moveLeft()
            KEY_ARROW_RIGHT -> editor.moveRight()
            KEY_ARROW_UP -> editor.moveUp()
            KEY_ARROW_DOWN -> editor.moveDown()
            KEY_HOME -> editor.goToStartOfLine()
            KEY_END -> editor.goToEndOfLine()
            else -> false
        }
        if (needUpdate) {
            updateHtml(editor)
        }
    }
    js("$(document)").click({ e: dynamic ->
        println("CLICKED ${e.pageX} ${e.pageY}")
        val left = js("$(\"#content\").offset().left") as Double
        val top = js("$(\"#content\").offset().top") as Double
        val height = js("$(\"#content\").height()") as Double
        val line = when {
            e.pageY < top -> 1
            e.pageY > top + height -> editor.numberOfLines()
            else -> {
                val lineHeight = height / editor.numberOfLines()
                Math.round((e.pageY - top) / lineHeight)
            }
        }
        editor.goTo(line, 0)
        updateHtml(editor)
        println("OFFSET $left $top")
        println("LINE $line")
    })
    println("..initialized")
}
