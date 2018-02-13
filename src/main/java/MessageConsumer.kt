package main.java

import javafx.animation.AnimationTimer
import javafx.scene.control.TextArea
import java.util.concurrent.BlockingQueue

class MessageConsumer constructor(private val messageQueue: BlockingQueue<String>,
                                  private val textArea: TextArea) : AnimationTimer() {

    override fun handle(now: Long) {
        val messages = mutableListOf<String>()
        messageQueue.drainTo(messages)
        messages.forEach {
            textArea.appendText("\n" + it)
        }
    }
}