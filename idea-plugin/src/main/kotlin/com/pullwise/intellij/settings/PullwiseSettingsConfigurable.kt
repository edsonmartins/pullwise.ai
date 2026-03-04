package com.pullwise.intellij.settings

import com.intellij.openapi.options.Configurable
import javax.swing.*

class PullwiseSettingsConfigurable : Configurable {

    private var apiUrlField: JTextField? = null
    private var projectIdField: JTextField? = null
    private var autoRefreshCheckBox: JCheckBox? = null
    private var severityFilterCombo: JComboBox<String>? = null
    private var panel: JPanel? = null

    override fun getDisplayName(): String = "Pullwise"

    override fun createComponent(): JComponent {
        panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            add(createLabeledField("API URL:", JTextField(30).also { apiUrlField = it }))
            add(Box.createVerticalStrut(8))
            add(createLabeledField("Project ID:", JTextField(30).also { projectIdField = it }))
            add(Box.createVerticalStrut(8))
            add(JCheckBox("Auto-refresh issues on file save").also { autoRefreshCheckBox = it })
            add(Box.createVerticalStrut(8))
            add(createLabeledField("Minimum Severity:",
                JComboBox(arrayOf("critical", "high", "medium", "low", "info")).also { severityFilterCombo = it }
            ))
            add(Box.createVerticalGlue())
        }

        reset()
        return panel!!
    }

    override fun isModified(): Boolean {
        val settings = PullwiseSettings.instance.state
        return apiUrlField?.text != settings.apiUrl
                || projectIdField?.text != settings.projectId
                || autoRefreshCheckBox?.isSelected != settings.autoRefresh
                || severityFilterCombo?.selectedItem as? String != settings.severityFilter
    }

    override fun apply() {
        val settings = PullwiseSettings.instance
        settings.loadState(PullwiseSettings.State(
            apiUrl = apiUrlField?.text ?: "http://localhost:8080/api",
            projectId = projectIdField?.text ?: "",
            autoRefresh = autoRefreshCheckBox?.isSelected ?: false,
            severityFilter = severityFilterCombo?.selectedItem as? String ?: "low"
        ))
    }

    override fun reset() {
        val settings = PullwiseSettings.instance.state
        apiUrlField?.text = settings.apiUrl
        projectIdField?.text = settings.projectId
        autoRefreshCheckBox?.isSelected = settings.autoRefresh
        severityFilterCombo?.selectedItem = settings.severityFilter
    }

    private fun createLabeledField(label: String, component: JComponent): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(JLabel(label))
            add(Box.createHorizontalStrut(8))
            add(component)
            alignmentX = JPanel.LEFT_ALIGNMENT
        }
    }
}
