package com.wasd.qlic

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class QlicBackendApiApplication

fun main(args: Array<String>) {
	runApplication<QlicBackendApiApplication>(*args)
}
