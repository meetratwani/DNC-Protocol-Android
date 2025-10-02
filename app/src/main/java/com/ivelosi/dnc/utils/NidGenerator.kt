package com.ivelosi.dnc.utils

import kotlin.random.Random

object NidGenerator {
    private val usedNids = mutableSetOf<Long>()

    fun generateNid(): Long {
        var nid: Long
        do {
            val firstDigit = Random.nextInt(1, 10) // 1..9, ensures no leading zero
            val remaining = (1..9)
                .map { Random.nextInt(0, 10) }
                .joinToString("")
            nid = "$firstDigit$remaining".toLong()
        } while (nid in usedNids)

        usedNids.add(nid)
        return nid
    }
}


fun main() {
    repeat(5) {
        println(NidGenerator.generateNid())
    }
}
