package be.ucclesport.rus

import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

fun String.field(start: Int, end: Int): String {
    return this.substring(start-1, end).trim { it <= ' ' }
}

interface CodaRecord

data class CodaTransaction(
    val sequenceNumber: String,
    val detailNumber: String,
    val bankReference: String,
    val isDebit: Boolean,
    val amount12_3: String,
    val valueDate: String,
    val transactionCode: String,
    val isCommunicationStructured: Boolean,
    val communication: String,
    val entryDate: String,
    val soaSequenceNumber: String,
    val globalisationCode: String,
    val isFollowedBy2Or3: Boolean,
    val isFollowedBy3: Boolean,
    val customerReference: String? = null,
    val bic: String? = null,
    val counterPartyAccountNumber: String? = null,
    val counterPartyName: String? = null,
    val isExtraCommunicationStructured: Boolean? = null,
    val extraCommunication: String? = null,
) : CodaRecord {
    companion object {
        operator fun invoke(line: String) = CodaTransaction(
            line.field(3,6),
            line.field(7,10),
            line.field(11,31),
            line.field(32,32) == "1",
            line.field(33,47),
            line.field(48,53),
            line.field(54,61),
            line.field(62,62) == "1",
            line.field(63,115),
            line.field(116,121),
            line.field(122,124),
            line.field(125,125),
            line.field(126,126) == "1",
            line.field(128,128) == "1",)
    }

    fun addRecord22(line: String) = this.copy(
        communication = this.communication + line.field(11,63),
        customerReference = line.field(64, 98),
        bic = line.field(99, 109),
    )

    fun addRecord23(line: String) = this.copy(
        counterPartyAccountNumber = line.field(11, 47),
        counterPartyName = line.field(48, 82),
    )

    override fun toString(): String {
        return "CodaTransaction(sequenceNumber='$sequenceNumber', detailNumber='$detailNumber', bankReference='$bankReference', isDebit=$isDebit, amount12_3='$amount12_3', valueDate='$valueDate', transactionCode='$transactionCode', isCommunicationStructured=$isCommunicationStructured, communication='$communication', entryDate='$entryDate', soaSequenceNumber='$soaSequenceNumber', globalisationCode='$globalisationCode', isFollowedBy2Or3=$isFollowedBy2Or3, isFollowedBy3=$isFollowedBy3, customerReference=$customerReference, bic=$bic, counterPartyAccountNumber=$counterPartyAccountNumber, counterPartyName=$counterPartyName, isExtraCommunicationStructured=$isExtraCommunicationStructured, extraCommunication=$extraCommunication)"
    }


}


fun parseCodaFile(filePath: String): List<CodaRecord> {
    val records: MutableList<CodaRecord> = java.util.ArrayList()
    try {
        BufferedReader(FileReader(filePath)).use { reader ->
            var latestTransaction: CodaTransaction? = null
            reader.forEachLine { line ->
                when (line[0]) {
                    '2' -> {
                        when (line[1]) {
                            '1' -> {
                                latestTransaction?.let { records.add(it) }
                                latestTransaction = CodaTransaction(line)
                            }
                            '2' -> latestTransaction = latestTransaction?.addRecord22(line)
                            '3' -> latestTransaction = latestTransaction?.addRecord23(line)
                        }

                    }
                    '3' -> {
                        latestTransaction = latestTransaction?.copy(
                            isExtraCommunicationStructured = line.field(40, 40) == "1",
                            extraCommunication = line.field(41, 113),
                        )
                    } // Transaction amount
                }
            }
            latestTransaction?.let { records.add(it) }
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return records
}


