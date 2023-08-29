package jsondatabase.server

import client.RequestDto
import client.ResponseDto
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.lang.Exception
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

//val dbfile = File("jsondatabase/server/data/db.json")
val dbfile = File("/Users/brazdahonza/IdeaProjects/projekty/JSON Database (Kotlin)/JSON Database (Kotlin)/task/src/jsondatabase/server/data/db.json")
fun main() {
    val myMap: MutableMap<String, String>
    if(dbfile.readText() != "") {
        val readJson = dbfile.readText()
        myMap = Json.decodeFromString(readJson)
    } else {
        myMap = mutableMapOf()
    }

    val address = "127.0.0.1"
    val port = 23456
    val server = ServerSocket(port, 50, InetAddress.getByName(address))
    println("Server started!")

    val executor: ExecutorService = Executors.newCachedThreadPool()
    val shouldExit = AtomicBoolean(false)

    try {
        while (!shouldExit.get()) {
            val socket = server.accept()
            executor.submit {
                handleClient(socket, myMap, shouldExit, server)
            }
        }
    } catch (e: SocketException) {
        println("Server stopped.")
    } finally {
        val json = Json.encodeToString(myMap)
        dbfile.writeText(json)
        executor.shutdown()
        server.close()
    }
}


fun handleClient(socket: Socket, myMap: MutableMap<String, String>, shouldExit: AtomicBoolean, server: ServerSocket) {
    socket.use {
        val output = DataOutputStream(it.getOutputStream())
        val input = DataInputStream(it.getInputStream())
        val lock: ReadWriteLock = ReentrantReadWriteLock()
        val readLock: Lock = lock.readLock()
        val writeLock: Lock = lock.writeLock()

        val rawMessage = input.readUTF()
        val jsonMessage = Json.decodeFromString<RequestDto>(rawMessage)
        println("Received: $jsonMessage")

        val clientMessage = "${jsonMessage.type} ${jsonMessage.key} ${jsonMessage.value}"
        var responseDto: ResponseDto
        val jsonResponseMessage: String

        val command = clientMessage.split(" ")
        when (command[0]) {
            "get" -> {
                responseDto = getElement(myMap, command, readLock)
                jsonResponseMessage = Json.encodeToString(responseDto)

                println("Sent: $jsonResponseMessage")
                output.writeUTF(jsonResponseMessage)
            }

            "delete" -> {
                responseDto = deleteElement(myMap, command, writeLock)
                jsonResponseMessage = Json.encodeToString(responseDto)

                println("Sent: $jsonResponseMessage")
                output.writeUTF(jsonResponseMessage)
            }

            "set" -> {
                responseDto = setElement(myMap, command, writeLock)
                jsonResponseMessage = Json.encodeToString(responseDto)

                println("Sent: $jsonResponseMessage")
                output.writeUTF(jsonResponseMessage)
            }

            "exit" -> {
                responseDto = ResponseDto("OK")
                jsonResponseMessage = Json.encodeToString(responseDto)
                println("Sent: $jsonResponseMessage")
                output.writeUTF(jsonResponseMessage)
                shouldExit.set(true)
                server.close()  // Close the ServerSocket to stop server.accept() from blocking
            }
            else -> {

            }
        }
    }
}

fun setElement(myMap: MutableMap<String, String>, command: List<String>, writeLock: Lock): ResponseDto {
    var key = ""
    writeLock.lock()
    try {
        key = command[1]
    } catch (e: Exception) {
        ResponseDto("ERROR")
    }

    var saveString = ""
    if (command.size < 3) ResponseDto("ERROR")

    for (i in 2..command.lastIndex) {
        saveString += "${command[i]} "
    }

    myMap[key] = saveString.trim()
    writeLock.unlock()
    return ResponseDto("OK")
}

fun deleteElement(myMap: MutableMap<String, String>, command: List<String>, writeLock: Lock): ResponseDto {
    writeLock.lock()
    val responseDto: ResponseDto
    var key = ""
    try {
        key = command[1]
    } catch (e: Exception) {
        writeLock.unlock()
        return ResponseDto("ERROR")
    }

    responseDto = try {
        if (myMap.get(key) == null) {
            throw Exception("No such key")
        }
        myMap.remove(key)
        ResponseDto("OK")
    } catch (e: Exception) {
        ResponseDto("ERROR", reason = "No such key")
    }
    writeLock.unlock()
    return responseDto
}

fun getElement(myMap: MutableMap<String, String>, command: List<String>, readLock: Lock): ResponseDto {
    readLock.lock()
    val responseDto: ResponseDto
    var key = "0"
    try {
        key = command[1]
    } catch (e: Exception) {
        readLock.unlock()
        return ResponseDto("ERROR")
    }

    responseDto = try {
        val value = myMap.get(key)!!
        ResponseDto("OK", value = value)
    } catch (e: Exception) {
        ResponseDto("ERROR", reason = "No such key")
    }
    readLock.unlock()
    return responseDto
}
