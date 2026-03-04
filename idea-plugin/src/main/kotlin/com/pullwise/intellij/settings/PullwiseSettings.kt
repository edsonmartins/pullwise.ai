package com.pullwise.intellij.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(name = "PullwiseSettings", storages = [Storage("pullwise.xml")])
class PullwiseSettings : PersistentStateComponent<PullwiseSettings.State> {

    private var myState = State()

    data class State(
        var apiUrl: String = "http://localhost:8080/api",
        var projectId: String = "",
        var autoRefresh: Boolean = false,
        var severityFilter: String = "low"
    )

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        val instance: PullwiseSettings
            get() = ApplicationManager.getApplication().getService(PullwiseSettings::class.java)
    }
}
