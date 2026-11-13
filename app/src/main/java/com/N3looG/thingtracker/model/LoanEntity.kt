package com.N3looG.thingtracker.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loan_items")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,               // Denumire obiect (ex: "Mașină de tuns iarba")
    val personName: String,          // Nume persoană (ex: "Ion")
    val phoneNumber: String? = null, // Număr de telefon asociat din agendă
    val loanType: LoanType,          // LENT sau BORROWED
    val startDate: Long,             // Data înregistrării (timestamp)
    val dueDate: Long? = null,       // Data scadenței (opțională)
    val hasAlarm: Boolean = false,   // Dacă are alarmă/notificare
    val isReturned: Boolean = false, // false = În derulare, true = Returnat/Arhivat
    val notes: String? = null,       // Observații opționale
    val imagePath: String? = null    // Cale locală poze obiect (separate prin |, max 3 poze)
)

fun LoanEntity.getImageList(): List<String> {
    return imagePath?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
}
