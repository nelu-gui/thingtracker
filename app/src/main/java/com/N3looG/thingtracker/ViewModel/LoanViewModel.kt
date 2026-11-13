package com.N3looG.thingtracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.N3looG.thingtracker.db.AppDatabase
import com.N3looG.thingtracker.model.LoanEntity
import com.N3looG.thingtracker.model.LoanType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class LoanViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).loanDao()

    fun getActiveLoans(type: LoanType): Flow<List<LoanEntity>> {
        return dao.getActiveLoans(type)
    }

    val returnedLoans: Flow<List<LoanEntity>> = dao.getReturnedLoans()

    fun insertLoan(loan: LoanEntity) {
        viewModelScope.launch {
            dao.insertLoan(loan)
        }
    }

    fun updateLoan(loan: LoanEntity) {
        viewModelScope.launch {
            dao.updateLoan(loan)
        }
    }

    fun deleteLoan(loan: LoanEntity) {
        viewModelScope.launch {
            dao.deleteLoan(loan)
        }
    }

    suspend fun exportToJson(): String {
        val loans = dao.getAllLoans()
        val jsonArray = JSONArray()
        for (loan in loans) {
            val obj = JSONObject().apply {
                put("id", loan.id)
                put("title", loan.title)
                put("personName", loan.personName)
                put("phoneNumber", loan.phoneNumber ?: JSONObject.NULL)
                put("loanType", loan.loanType.name)
                put("startDate", loan.startDate)
                put("dueDate", loan.dueDate ?: JSONObject.NULL)
                put("hasAlarm", loan.hasAlarm)
                put("isReturned", loan.isReturned)
                put("notes", loan.notes ?: JSONObject.NULL)
                put("imagePath", loan.imagePath ?: JSONObject.NULL)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString(2)
    }

    suspend fun importFromJson(jsonString: String) {
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val loan = LoanEntity(
                    id = obj.optLong("id", 0),
                    title = obj.getString("title"),
                    personName = obj.getString("personName"),
                    phoneNumber = if (obj.isNull("phoneNumber")) null else obj.getString("phoneNumber"),
                    loanType = LoanType.valueOf(obj.getString("loanType")),
                    startDate = obj.getLong("startDate"),
                    dueDate = if (obj.isNull("dueDate")) null else obj.getLong("dueDate"),
                    hasAlarm = obj.optBoolean("hasAlarm", false),
                    isReturned = obj.optBoolean("isReturned", false),
                    notes = if (obj.isNull("notes")) null else obj.getString("notes"),
                    imagePath = if (obj.isNull("imagePath")) null else obj.getString("imagePath")
                )
                dao.insertLoan(loan)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
