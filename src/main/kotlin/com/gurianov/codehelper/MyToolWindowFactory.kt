package com.gurianov.codehelper

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import javax.swing.*
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

class MyToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val service = project.getService(MyService::class.java)

        val codeArea = JTextArea("Select code in the editor and right-click → Explain").apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            font = Font("Monospaced", Font.PLAIN, 12)
        }

        val chatPane = JTextPane().apply { isEditable = false }
        val doc = chatPane.styledDocument

        val youStyle = SimpleAttributeSet().apply {
            StyleConstants.setForeground(this, Color(70, 130, 220))
            StyleConstants.setBold(this, true)
        }
        val aiStyle = SimpleAttributeSet().apply {
            StyleConstants.setForeground(this, Color(210, 70, 70))
            StyleConstants.setBold(this, true)
        }
        val systemStyle = SimpleAttributeSet().apply {
            StyleConstants.setForeground(this, Color(150, 150, 150))
            StyleConstants.setBold(this, true)
        }
        val normalStyle = SimpleAttributeSet()

        fun appendToChat(prefix: String, message: String, prefixStyle: SimpleAttributeSet) {
            doc.insertString(doc.length, "$prefix ", prefixStyle)
            doc.insertString(doc.length, "$message\n\n", normalStyle)
            chatPane.caretPosition = doc.length
        }

        val inputField = JTextField().apply { toolTipText = "Type a message..." }
        val sendButton = JButton("Send")

        fun sendMessage() {
            val text = inputField.text.trim()
            if (text.isEmpty()) return
            appendToChat("You:", text, youStyle)
            appendToChat("System:", "Thinking...", systemStyle)
            inputField.text = ""
            service.sendChatMessage(text)
        }

        sendButton.addActionListener { sendMessage() }
        inputField.addActionListener { sendMessage() }

        service.addCodeListener { code ->
            SwingUtilities.invokeLater {
                codeArea.text = code
                appendToChat("System:", "Analyzing ${code.lines().size} lines of code...", systemStyle)
            }
        }

        service.addResponseListener { response ->
            SwingUtilities.invokeLater {
                appendToChat("AI:", response, aiStyle)
            }
        }

        val topPanel = JPanel(BorderLayout()).apply {
            add(JLabel("  Selected code:"), BorderLayout.NORTH)
            add(JScrollPane(codeArea), BorderLayout.CENTER)
        }

        val inputPanel = JPanel(BorderLayout()).apply {
            add(inputField, BorderLayout.CENTER)
            add(sendButton, BorderLayout.EAST)
        }

        val bottomPanel = JPanel(BorderLayout()).apply {
            add(JLabel("  Chat:"), BorderLayout.NORTH)
            add(JScrollPane(chatPane), BorderLayout.CENTER)
            add(inputPanel, BorderLayout.SOUTH)
        }

        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, bottomPanel).apply {
            resizeWeight = 0.25
            dividerSize = 5
        }

        val panel = JPanel(BorderLayout()).apply {
            add(splitPane, BorderLayout.CENTER)
        }

        val content = ContentFactory.getInstance().createContent(panel, "Chat", false)
        toolWindow.contentManager.addContent(content)
    }
}
