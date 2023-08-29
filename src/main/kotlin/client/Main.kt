package client
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.Socket
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.File

fun main(args: Array<String>) {

    val address = "127.0.0.1"
    val port = 23456
    val socket = Socket(InetAddress.getByName(address), port)
    println("Client started!")
    val input = DataInputStream(socket.getInputStream())
    val output = DataOutputStream(socket.getOutputStream())


    var operation = ""
    var key = ""
    var value = ""
    var fileName = ""
    for(i in 0..args.lastIndex step 2) {
        if(args[i] == "-t") {
            operation = args[i+1]
        } else if(args[i] == "-k") {
            key = args[i+1]
        } else if(args[i] == "-v") {
            for(j in i+1..args.lastIndex) {
                value += "${args[j]} "
            }
        } else if(args[i] == "-in") {
            fileName = args[i+1]
        }

    }

    val requestDto: RequestDto
    var serialized = ""
    if(fileName != ""){
        val file = File("/Users/brazdahonza/IdeaProjects/projekty/JSON Database (Kotlin)/JSON Database (Kotlin)/task/src/jsondatabase/client/data/$fileName")
//        val file = File("jsondatabase/client/$fileName")
        serialized = file.readText()
    } else {
        if (value != "") {
            requestDto = RequestDto(operation, key, value.trim())
        } else {
            requestDto = RequestDto(operation, key)
        }

        serialized = Json.encodeToString(requestDto)
    }
    output.writeUTF(serialized)
    println("Sent: $serialized")

    val clientMessage = input.readUTF()
    println("Received: $clientMessage")
}
