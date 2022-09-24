package com.wahyurhy.transactiontracker.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.wahyurhy.transactiontracker.utils.NAME_WORKER_EXTRA

class TransactionWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private val TAG = TransactionWorker::class.java.simpleName

    private var resultStatus: Result? = null

    override fun doWork(): Result {
        val nameClient = inputData.getString(NAME_WORKER_EXTRA)
        return createNewTransactionNextMonth(nameClient)
    }

    private fun createNewTransactionNextMonth(nameClient: String?): Result {
        try {
            resultStatus = Result.success()
        } catch (e: Exception) {
            resultStatus = Result.failure()
        }
        return resultStatus as Result
    }
}