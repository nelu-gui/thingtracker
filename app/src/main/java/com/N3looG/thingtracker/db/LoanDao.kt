package com.N3looG.thingtracker.db

import androidx.room.*
import com.N3looG.thingtracker.model.LoanEntity
import com.N3looG.thingtracker.model.LoanType
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {

    // Returnează toate obiectele după tip (LENT sau BORROWED), ordonate mai întâi cele ne-returnate (active), apoi după scadență
    @Query("SELECT * FROM loan_items WHERE loanType = :type ORDER BY isReturned ASC, CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END, dueDate ASC")
    fun getActiveLoans(type: LoanType): Flow<List<LoanEntity>>

    // Returnează istoricul obiectelor returnate (Arhivă)
    @Query("SELECT * FROM loan_items WHERE isReturned = 1 ORDER BY startDate DESC")
    fun getReturnedLoans(): Flow<List<LoanEntity>>

    // Returnează toate obiectele pentru backup
    @Query("SELECT * FROM loan_items ORDER BY startDate DESC")
    suspend fun getAllLoans(): List<LoanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity)

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Delete
    suspend fun deleteLoan(loan: LoanEntity)
}
