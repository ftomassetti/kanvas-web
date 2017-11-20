import jquery.JQuery

class Editor(initialText: String = "", initialIndex: Int = 0) {
    private var caretIndex: Int = initialIndex
    private var nLines: Int
    private var text: String = initialText

    fun textBeforeCaret() : String {
        return if (this.caretIndex == 0) {
            ""
        } else {
            this.text.substring(0, this.caretIndex)
        }
    }

    fun textAfterCaret() : String {
        return if (this.caretIndex  == this.text.length) {
            ""
        } else {
            this.text.substring(this.caretIndex );
        }
    }

    fun currentLine() : Int {
        return this.textBeforeCaret().lines().size
    }

    fun currentIndex() : Int {
        return this.caretIndex;
    }

    fun numberOfLines() : Int  {
        return this.nLines
    }

    fun currentColumn() : Int  {
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

    private fun goTo(line: Int, column: Int) {
        var l = line
        var c = column
        var newIndex = 0
        if (line >= this.numberOfLines()) {
            l = this.numberOfLines() - 1
        }
        if (column > this.numberOfColumnsForLine(line)) {
            c = this.numberOfColumnsForLine(line)
        }
        for (i in 0.rangeTo(l)) {
            newIndex = this.text.indexOf("\n", newIndex) + 1
        }
        newIndex += c
        this.caretIndex = newIndex
    }

    private fun toHtml(text: String) : String {
        return text.replace("\n", "<br/>")
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

    fun goToStartOfLine() {
        this.goTo(this.currentLine(), 0)
    }

    fun goToEndOfLine() {
        this.goTo(this.currentLine(), this.numberOfColumnsForLine(this.currentLine()))
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

fun main(args: Array<String>) {
    println("Starting...")
    val editor = Editor()
    updateHtml(editor)
    js("$(document)").keypress({ e : dynamic ->
        println("PRESSED ${e.which}")
        val c = if (e.which == 13) "\n" else js("String.fromCharCode(e.which)") as String
        editor.type(c)
        updateHtml(editor)
    })
    js("$(document)").keydown({ e : dynamic ->
        if (e.which == 46 && editor.deleteNextChar()) {
            updateHtml(editor)
        }
        if (e.which == 8 && editor.deletePrevChar()) {
            updateHtml(editor)
        }
        if (e.which == 37 && editor.moveLeft()) {
            updateHtml(editor)
        }
        if (e.which == 39 && editor.moveRight()) {
            updateHtml(editor)
        }
        if (e.which == 38 && editor.moveUp()) {
            updateHtml(editor)
        }
        if (e.which == 40 && editor.moveDown()) {
            updateHtml(editor)
        }
        if (e.which == 36) {
            editor.goToStartOfLine()
            updateHtml(editor)
        }
        if (e.which == 35) {
            editor.goToEndOfLine()
            updateHtml(editor)
        }
    })
    println("..initialized")
}
