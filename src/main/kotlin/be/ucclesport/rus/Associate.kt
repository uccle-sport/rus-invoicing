#!/usr/bin/env kotlinc -script

package be.ucclesport.rus

import java.io.File
import java.text.Normalizer
import java.util.Locale
import kotlin.math.min

val linesToKeep = setOf(
    "Affiliation Hockey",
    "Hockey - Adultes",
    "Hockey - Adultes sans entraïnements",
    "Hockey - cotisation arbitre théorique",
    "Hockey - Jeunes Adultes 1998-2004",
    "Hockey - Ladies/Gentlemen",
    "Hockey - Non joueurs",
    "Hockey - Poussins",
    "Hockey - réduction 2è enfant",
    "Hockey - réduction 3é enfant",
    "Hockey - réduction famille",
    "Hockey - Top Hockey",
    "Hockey - U 7-U 8",
    "Hockey - U 9-U12",
    "Hockey - U14-U19",
    "Hockey Together"
)

fun main(args: Array<String>) {
    val fromAccounting = mutableSetOf<Pair<String,String>>()
    val fromErp = mutableSetOf<Triple<String,String,String>>()

    (args.takeIf { it.size > 1 }?.get(1)?.let { File(it).inputStream() } ?: throw IllegalArgumentException("Missing argument")).bufferedReader().use { reader ->
        reader.lines().skip(1).forEach {
            val fields = it.split(";")
            val code = fields[0]
            val name = fields[1]
            fromAccounting.add(Pair(code, name))
        }
    }
    (args.get(0).let { File(it).inputStream() } ).bufferedReader().use { reader ->
        reader.lines().skip(1).forEach {
            val fields = it.split(";")
            val name = fields[1]
            val phone = fields[2]
            val email = fields[3]
            val description = fields[23]

            if (linesToKeep.any { line -> description.contains(line) }) {
                fromErp.add(Triple(name, phone, email))
            }
        }
    }

    fromErp.forEach { erp ->
        val sorted = fromAccounting.sortedWith(Comparator<Pair<String, String>> { o1, o2 ->
            val o1Code = o1.first.lowercase(Locale.getDefault())
            val o2Code = o2.first.lowercase(Locale.getDefault())

            val o1Name = o1.second.lowercase(Locale.getDefault()).normalize().replace(" ","")
            val o2Name = o2.second.lowercase(Locale.getDefault()).normalize().replace(" ","")

            val erpName = erp.first.lowercase(Locale.getDefault()).normalize()
            val erpEmail = erp.third.lowercase(Locale.getDefault()).normalize().replace(" ","")

            val o1Common = (erpName.split(" ") + listOf(erpName.replace(" ",""))).permutations().maxOf { o1Name.commonPrefixWith(it.joinToString("")).length }
            val o2Common = (erpName.split(" ") + listOf(erpName.replace(" ",""))).permutations().maxOf { o2Name.commonPrefixWith(it.joinToString("")).length }

            if (o1Common != o2Common) {
                o2Common - o1Common
            } else {
                val o1FromName = o1Name.levenshtein(erpName)
                val o2FromName = o2Name.levenshtein(erpName)

                if (o1FromName != o2FromName) {
                    o1FromName - o2FromName
                } else {
                    o1Name.levenshtein(erpEmail) - o2Name.levenshtein(erpEmail)
                }
            }
        })
        val acc = sorted.first()

        println(erp.toList().joinToString(";") + ";" + acc.toList().joinToString(";"))
    }
}

fun String.normalize(): String = this.lowercase(Locale.getDefault()).unaccent().replace(Regex("[\t ]+"), " ").replace(Regex("[^a-zA-Z ]"), "")

fun CharSequence.unaccent(): String {
    val REGEX_UNACCENT = "\\p{InCombiningDiacriticalMarks}+".toRegex()
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    return REGEX_UNACCENT.replace(temp, "")
}

fun CharSequence.levenshtein(rhs: CharSequence) : Int {
    if(this == rhs) { return 0 }
    if(isEmpty()) { return rhs.length }
    if(rhs.isEmpty()) { return length }

    val lhsLength = length + 1
    val rhsLength = rhs.length + 1

    var cost = Array(lhsLength) { it }
    var newCost = Array(lhsLength) { 0 }

    for (i in 1..rhsLength-1) {
        newCost[0] = i

        for (j in 1..lhsLength-1) {
            val match = if(this[j - 1] == rhs[i - 1]) 0 else 1

            val costReplace = cost[j - 1] + match
            val costInsert = cost[j] + 1
            val costDelete = newCost[j - 1] + 1

            newCost[j] = min(min(costInsert, costDelete), costReplace)
        }

        val swap = cost
        cost = newCost
        newCost = swap
    }

    return cost[lhsLength - 1]
}

fun List<String>.permutations(): List<List<String>> = if (this.count() == 1) listOf(this) else this.flatMapIndexed { index, s ->
    (this.subList(0, index) + this.subList(index+1, this.count())).permutations().map {
        listOf(s) + it
    }
}
