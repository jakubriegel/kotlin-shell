package eu.jrie.jetbrains.kotlinshell.testutils

object TestDataFactory {
    const val VIRTUAL_PID = 1
    const val PROCESS_NAME = "[name]"

    const val PROCESS_COMMAND = "some_cmd"
    val PROCESS_ARGS = listOf("arg1", "arg2", "arg3")

    val ENV_VAR_1 = "env1" to "value1"
    val ENV_VAR_2 = "env2" to "value2"
    val ENV_VAR_3 = "env3" to "value3"
    val ENVIRONMENT = mapOf(ENV_VAR_1, ENV_VAR_2, ENV_VAR_3)

    const val SYSTEM_PID: Long = 1001
}
