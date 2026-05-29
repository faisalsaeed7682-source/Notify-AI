package com.example.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import com.example.ui.screens.DiHelper

class ClassificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("ClassificationWorker", "Running background classification...")
        try {
            val repository = DiHelper.getRepository(applicationContext)
            
            Log.d("ClassificationWorker", "Background classification completed successfully.")
            return Result.success()
        } catch (e: Exception) {
            Log.e("ClassificationWorker", "Error classifying notifications", e)
            return Result.retry()
        }
    }

    companion object {
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<ClassificationWorker>(1, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "classification_work",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
