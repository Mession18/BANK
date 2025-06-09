package com.example.incomeplanner.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.incomeplanner.R
import com.example.incomeplanner.model.IncomeEntry
import com.example.incomeplanner.service.InMemoryIncomeService
import com.example.incomeplanner.service.IncomeService
import com.example.incomeplanner.service.Result
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class AddIncomeActivity : AppCompatActivity() {

    private lateinit var incomeAmountEditText: EditText
    private lateinit var datePickerButton: Button
    private lateinit var incomeDescriptionEditText: EditText
    private lateinit var saveIncomeButton: Button

    private val selectedDateCalendar: Calendar = Calendar.getInstance()
    // incomeService will be initialized in onCreate using applicationContext
    private lateinit var incomeService: IncomeService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomeService = InMemoryIncomeService.getInstance(applicationContext)
        setContentView(R.layout.activity_add_income)

        incomeAmountEditText = findViewById(R.id.incomeAmountEditText)
        datePickerButton = findViewById(R.id.datePickerButton)
        incomeDescriptionEditText = findViewById(R.id.incomeDescriptionEditText)
        saveIncomeButton = findViewById(R.id.saveIncomeButton)

        updateDatePickerButtonText()

        datePickerButton.setOnClickListener {
            showDatePickerDialog()
        }

        saveIncomeButton.setOnClickListener {
            saveIncomeEntry()
        }
    }

    private fun updateDatePickerButtonText() {
        val sdf = SimpleDateFormat("EEE, MMM dd yyyy", Locale.getDefault())
        datePickerButton.text = sdf.format(selectedDateCalendar.time)
    }

    private fun showDatePickerDialog() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            selectedDateCalendar.set(Calendar.YEAR, year)
            selectedDateCalendar.set(Calendar.MONTH, month)
            selectedDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDatePickerButtonText()
        }

        DatePickerDialog(
            this,
            dateSetListener,
            selectedDateCalendar.get(Calendar.YEAR),
            selectedDateCalendar.get(Calendar.MONTH),
            selectedDateCalendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveIncomeEntry() {
        val amountString = incomeAmountEditText.text.toString()
        val description = incomeDescriptionEditText.text.toString().takeIf { it.isNotBlank() }

        if (amountString.isBlank()) {
            incomeAmountEditText.error = "Amount cannot be empty"
            return
        }

        val amount = amountString.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            incomeAmountEditText.error = "Please enter a valid positive amount"
            return
        }
        incomeAmountEditText.error = null // Clear error

        val newEntry = IncomeEntry(
            id = UUID.randomUUID().toString(),
            amount = amount,
            date = selectedDateCalendar.timeInMillis,
            description = description
        )

        // Use lifecycleScope to launch a coroutine for the suspend function call
        androidx.lifecycle.lifecycleScope.launch {
            when (val result = incomeService.addIncomeEntry(newEntry)) {
                is Result.Success -> {
                    Toast.makeText(this@AddIncomeActivity, "Income saved successfully!", Toast.LENGTH_SHORT).show()
                    finish() // Close activity on success
                }
                is Result.Error -> {
                    val errorMessage = result.exception.message ?: "Unknown error occurred"
                    com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), "Error saving income: $errorMessage", com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }
}

// Note: The InMemoryIncomeServiceSingleton object and comments below are now outdated
// as InMemoryIncomeService.getInstance() now takes context and returns the Room-backed version.
