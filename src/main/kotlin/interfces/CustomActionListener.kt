package interfces

import java.util.*


interface CustomActionListener : EventListener {
    fun actionPerformed(event: CustomEvent)
}