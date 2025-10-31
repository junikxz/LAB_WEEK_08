package com.example.lab_week_08.worker

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters

class ThirdWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        // Get the parameter input
        val id = inputData.getString(INPUT_DATA_ID)
        Log.d("ThirdWorker", "ThirdWorker starting with ID: $id")

        // Simulate a background process (e.g., 3 seconds)
        Thread.sleep(3000L)

        // Build the output based on process result
        val outputData = Data.Builder()
            .putString(OUTPUT_DATA_ID, id)
            .build()

        Log.d("ThirdWorker", "ThirdWorker finished.")
        // Return the output
        return Result.success(outputData)
    }

    companion object {
        const val INPUT_DATA_ID = "inId"
        const val OUTPUT_DATA_ID = "outId"
    }
}
