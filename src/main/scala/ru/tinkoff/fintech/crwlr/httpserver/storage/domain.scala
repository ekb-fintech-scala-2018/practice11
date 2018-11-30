package ru.tinkoff.fintech.crwlr.httpserver.storage

case class Job(id: String, isCompleted: Boolean)

case class HostCounter(jobId: String, host: String, counter: Int)