package com.wahyurhy.transactiontracker.ui.fragments

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.wahyurhy.transactiontracker.R
import com.wahyurhy.transactiontracker.adapter.TransactionAdapter
import com.wahyurhy.transactiontracker.data.source.local.model.TransactionModel
import com.wahyurhy.transactiontracker.databinding.FragmentTransactionBinding
import com.wahyurhy.transactiontracker.databinding.MarkDialogBinding
import com.wahyurhy.transactiontracker.notification.MonthlyNotification
import com.wahyurhy.transactiontracker.ui.details.DetailMessagesActivity
import com.wahyurhy.transactiontracker.ui.details.TransactionDetailsActivity
import com.wahyurhy.transactiontracker.ui.main.MainActivity
import com.wahyurhy.transactiontracker.utils.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*


class TransactionFragment : Fragment() {

    private val user = Firebase.auth.currentUser
    private lateinit var dbRef: DatabaseReference
    private var selectedTimeSpan: String = sdf.format(currentYear) // default year
    private var selectedMonth = ""
    private var selectedShowStatus: String =
        arrayOf(R.array.filter_sort_by_status)[0].toString() // default all time
    private var year: String? = ""
    private var getYear: Int = 0
    private var dateStart: Long = 0
    private var dateEnd: Long = 0
    private var totalRevenue = 0.0
    private var totalTransaction = 0
    private var selectedYear = ""
    private var isSearched = false
    private var isWithMonth = false
    private var isFirstLaunch = true
    private val searchText = StringBuilder()
    private lateinit var transactionList: ArrayList<TransactionModel>
    private lateinit var messages: ArrayList<String>
    private lateinit var valueEventListenerGetTransactionData: ValueEventListener
    private lateinit var valueEventListenerSearch: ValueEventListener
    private lateinit var queryGetTransactionData: Query
    private lateinit var querySearch: Query
    private var transactionData: TransactionModel? = null
    private var calendar: Calendar? = null
    private var dateFormat: SimpleDateFormat? = null
    private var formatRupiah: NumberFormat? = null
    private var email: String? = null
    private var username: String? = null
    private var name: String? = null
    private var splitValue: List<String>? = null

    private var _binding: FragmentTransactionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            // Inflate the layout for this fragment
            selectedMonth =
                resources.getStringArray(R.array.filter_sort_by_month)[0].toString() // default month
            _binding = FragmentTransactionBinding.inflate(inflater, container, false)
        } catch (e: Exception) {
            startActivity(Intent(requireContext(), MainActivity::class.java))
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        calendar = Calendar.getInstance()
        dateFormat = SimpleDateFormat("yyyy", Locale("in", "ID"))
        formatRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))

        binding.notifButton.setOnClickListener {
            messages = arrayListOf()
            isFirstLaunch = true
            getTransactionData()
            showCurrentNotification(requireContext(), messages)
        }

        showUserName()
        setCurrentYear()
        visibilityOptions()

        initTransactionRecyclerView()
        getTransactionData()
        setSearchBarListener()
        setSwipeRefreshListener()
    }

    private fun setSwipeRefreshListener() {
        binding.swipeRefresh.setOnRefreshListener {
            getTransactionData()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun setSearchBarListener() {
        binding.searchBar.setOnQueryTextListener(object : OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText != "") {
                    val uid = user?.uid
                    if (uid != null) {
                        dbRef = FirebaseDatabase.getInstance().getReference(uid)
                    }

                    valueEventListenerSearch = object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            transactionList.clear()
                            if (snapshot.exists()) {
                                when (selectedShowStatus) {
                                    resources.getStringArray(R.array.filter_sort_by_status)[0].toString() -> {
                                        for (transactionSnap in snapshot.children) {
                                            transactionData =
                                                transactionSnap.getValue(TransactionModel::class.java)

                                            calendar?.timeInMillis =
                                                transactionData!!.dueDateTransaction as Long

                                            year = dateFormat?.format(calendar!!.time)

                                            if (year == selectedYear) {
                                                transactionList.add(transactionData!!)
                                            }
                                        }
                                    }
                                    resources.getStringArray(R.array.filter_sort_by_status)[1].toString() -> {
                                        for (transactionSnap in snapshot.children) {
                                            transactionData =
                                                transactionSnap.getValue(TransactionModel::class.java)

                                            calendar?.timeInMillis =
                                                transactionData!!.dueDateTransaction as Long

                                            year = dateFormat?.format(calendar!!.time)

                                            if (transactionData!!.stateTransaction!!) {
                                                if (year == selectedYear) {
                                                    transactionList.add(transactionData!!)
                                                }
                                            }
                                        }
                                    }
                                    resources.getStringArray(R.array.filter_sort_by_status)[2].toString() -> {
                                        for (transactionSnap in snapshot.children) {
                                            transactionData =
                                                transactionSnap.getValue(TransactionModel::class.java)

                                            calendar?.timeInMillis =
                                                transactionData!!.dueDateTransaction as Long

                                            val year = dateFormat?.format(calendar!!.time)

                                            if (!transactionData!!.stateTransaction!!) {
                                                if (year == selectedYear) {
                                                    transactionList.add(transactionData!!)
                                                }
                                            }
                                        }
                                    }
                                }
                                showInAdapter(false)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            throw Exception("Not yet implemented")
                        }
                    }

                    getSearchData(newText.trim())
                    searchText.replace(0, searchText.length, newText.trim())
                    isSearched = true
                } else {
                    getTransactionData()
                }
                return true
            }
        })
    }

    private fun initTransactionRecyclerView() {
        // -- Recycler View transaction items --
        binding.rvTransaction.layoutManager = LinearLayoutManager(this.activity)
        binding.rvTransaction.setHasFixedSize(true)
        transactionList = arrayListOf()
        messages = arrayListOf()
    }

    private fun getSearchData(text: String) {
        querySearch = dbRef.orderByChild("name").startAt(text)
        querySearch.addListenerForSingleValueEvent(valueEventListenerSearch)
    }

    private fun setCurrentYear() {
        for (i in 0..11) {
            when (selectedTimeSpan) {
                resources.getStringArray(R.array.filter_sort_by_periode)[i].toString() -> getYear =
                    i + 1
            }
            binding.timeShowSpinner.setSelection(getYear - 1)
        }
    }

    private fun visibilityOptions() {
        binding.timeShowSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    for (i in 0..11) {
                        when (binding.timeShowSpinner.selectedItem) {
                            resources.getStringArray(R.array.filter_sort_by_periode)[i].toString() -> {
                                binding.tvDesc.text = getString(
                                    R.string.info_revenue,
                                    resources.getStringArray(R.array.filter_sort_by_periode)[i].toString()
                                )
                                selectedTimeSpan =
                                    resources.getStringArray(R.array.filter_sort_by_periode)[i].toString()
                                selectedYear =
                                    resources.getStringArray(R.array.filter_sort_by_periode)[i].toString()

                                if (isWithMonth) {
                                    var bulan = 0
                                    when (selectedMonth) {
                                        "January" -> bulan = Bulan.January.value
                                        "Januari" -> bulan = Bulan.January.value
                                        "February" -> bulan = Bulan.February.value
                                        "Februari" -> bulan = Bulan.February.value
                                        "March" -> bulan = Bulan.March.value
                                        "Maret" -> bulan = Bulan.March.value
                                        "April" -> bulan = Bulan.April.value
                                        "May" -> bulan = Bulan.May.value
                                        "Mei" -> bulan = Bulan.May.value
                                        "June" -> bulan = Bulan.June.value
                                        "Juni" -> bulan = Bulan.June.value
                                        "July" -> bulan = Bulan.July.value
                                        "Juli" -> bulan = Bulan.July.value
                                        "August" -> bulan = Bulan.August.value
                                        "Agustus" -> bulan = Bulan.August.value
                                        "September" -> bulan = Bulan.September.value
                                        "October" -> bulan = Bulan.October.value
                                        "Oktober" -> bulan = Bulan.October.value
                                        "November" -> bulan = Bulan.November.value
                                        "December" -> bulan = Bulan.December.value
                                        "Desember" -> bulan = Bulan.December.value
                                    }
                                    getRangeDate(Calendar.DAY_OF_MONTH, bulan)
                                } else {
                                    getRangeDate(Calendar.YEAR, 0)
                                }
                            }
                        }
                        getTransactionData()
                        showEmptyRevenue()
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    TODO("Not yet implemented")
                }
            }

        binding.monthSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    for (i in 0..12) {
                        when (binding.monthSpinner.selectedItem) {
                            resources.getStringArray(R.array.filter_sort_by_month)[i].toString() -> {
                                selectedMonth =
                                    resources.getStringArray(R.array.filter_sort_by_month)[i].toString()

                                if (i == 0) {
                                    isWithMonth = false
                                    binding.tvDesc.text =
                                        getString(R.string.info_revenue, selectedYear)
                                    getRangeDate(Calendar.YEAR, 0)
                                } else {
                                    isWithMonth = true
                                    binding.tvDesc.text = getString(
                                        R.string.info_revenue_with_month,
                                        resources.getStringArray(R.array.filter_sort_by_month)[i].toString(),
                                        selectedYear
                                    )
                                    getRangeDate(Calendar.DAY_OF_MONTH, i - 1)
                                }
                            }
                        }
                        getTransactionData()
                        showEmptyRevenue()
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    TODO("Not yet implemented")
                }
            }

        binding.typeShowSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    for (i in 0..2) {
                        when (binding.typeShowSpinner.selectedItem) {
                            resources.getStringArray(R.array.filter_sort_by_status)[i].toString() -> {
                                selectedShowStatus =
                                    resources.getStringArray(R.array.filter_sort_by_status)[i].toString()
                            }
                        }
                        getTransactionData()
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    TODO("Not yet implemented")
                }
            }
    }

    private fun showEmptyRevenue() {
        binding.numberOfRevenue.text = formatRupiah?.format(totalRevenue)?.replace(",00", "")
        totalTransaction = 0
    }

    private fun getTransactionData() {
        binding.apply {
            shimmerFrameLayout.startShimmerAnimation()
            shimmerFrameLayout.visibility = View.VISIBLE
            noDataImage.visibility = View.GONE
            tvNoDataTitle.visibility = View.GONE
            tvNoData.visibility = View.GONE
            visibilityNoData.visibility = View.GONE
            rvTransaction.visibility = View.GONE
        }

        val uid = user?.uid
        if (uid != null) {
            dbRef = FirebaseDatabase.getInstance().getReference(uid)
        }

        queryGetTransactionData = dbRef.orderByChild("dueDateTransaction")

        valueEventListenerGetTransactionData = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                transactionList.clear()
                if (snapshot.exists()) {
                    when (selectedShowStatus) {
                        resources.getStringArray(R.array.filter_sort_by_status)[0].toString() -> {
                            for (transactionSnap in snapshot.children) {
                                transactionData =
                                    transactionSnap.getValue(TransactionModel::class.java)

                                calendar?.timeInMillis =
                                    transactionData!!.dueDateTransaction as Long

                                year = dateFormat?.format(calendar!!.time)

                                if (isWithMonth) {
                                    if (transactionData!!.dueDateTransaction!! > dateStart - 86400000 && transactionData!!.dueDateTransaction!! <= dateEnd) {
                                        showInstantNotification()
                                        transactionList.add(transactionData!!)
                                    }
                                } else {
                                    if (year == selectedYear) {
                                        showInstantNotification()
                                        transactionList.add(transactionData!!)
                                    }
                                }
                            }
                        }
                        resources.getStringArray(R.array.filter_sort_by_status)[1].toString() -> {
                            for (transactionSnap in snapshot.children) {
                                transactionData =
                                    transactionSnap.getValue(TransactionModel::class.java)

                                calendar?.timeInMillis =
                                    transactionData!!.dueDateTransaction as Long

                                year = dateFormat?.format(calendar!!.time)

                                if (transactionData!!.stateTransaction!!) {
                                    if (isWithMonth) {
                                        if (transactionData!!.dueDateTransaction!! > dateStart - 86400000 && transactionData!!.dueDateTransaction!! <= dateEnd) {
                                            showInstantNotification()
                                            transactionList.add(transactionData!!)
                                        }
                                    } else {
                                        if (year == selectedYear) {
                                            showInstantNotification()
                                            transactionList.add(transactionData!!)
                                        }
                                    }
                                }
                            }
                        }
                        resources.getStringArray(R.array.filter_sort_by_status)[2].toString() -> {
                            for (transactionSnap in snapshot.children) {
                                transactionData =
                                    transactionSnap.getValue(TransactionModel::class.java)

                                calendar?.timeInMillis =
                                    transactionData!!.dueDateTransaction as Long

                                year = dateFormat?.format(calendar!!.time)

                                if (!transactionData!!.stateTransaction!!) {
                                    if (isWithMonth) {
                                        if (transactionData!!.dueDateTransaction!! > dateStart - 86400000 && transactionData!!.dueDateTransaction!! <= dateEnd) {
                                            showInstantNotification()
                                            transactionList.add(transactionData!!)
                                        }
                                    } else {
                                        if (year == selectedYear) {
                                            showInstantNotification()
                                            transactionList.add(transactionData!!)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (transactionList.isEmpty()) {
                        binding.apply {
                            noDataImage.visibility = View.VISIBLE
                            tvNoDataTitle.visibility = View.VISIBLE
                            visibilityNoData.visibility = View.VISIBLE
                            visibilityNoData.text = resources.getString(
                                R.string.visibility_no_data_info,
                                selectedShowStatus,
                                selectedTimeSpan
                            )
                        }
                    } else {
                        showInAdapter(true)
                    }
                    binding.apply {
                        shimmerFrameLayout.stopShimmerAnimation()
                        shimmerFrameLayout.visibility = View.GONE
                    }
                } else {
                    binding.apply {
                        shimmerFrameLayout.stopShimmerAnimation()
                        shimmerFrameLayout.visibility = View.GONE

                        noDataImage.visibility = View.VISIBLE
                        tvNoDataTitle.visibility = View.VISIBLE
                        tvNoData.visibility = View.VISIBLE
                    }
                }
                isFirstLaunch = false
            }

            override fun onCancelled(error: DatabaseError) {
                print("Listener was cancelled")
                Log.d("TransactionFragment", "Listener was cancelled ${error.message}")
            }
        }
        queryGetTransactionData.addValueEventListener(valueEventListenerGetTransactionData)
    }

    private fun showInstantNotification() {
        if (isFirstLaunch) {
            if (transactionData!!.amountLeft!! > 0.0) {
                if (transactionData!!.amountLeft!! < transactionData!!.paymentAmount!!) {
                    messages.add(
                        resources.getString(
                            R.string.messageLeftInfo,
                            transactionData!!.name,
                            formatRupiah?.format(
                                transactionData!!.amountLeft
                            )?.replace(",00", "")
                        )
                    )
                    showCurrentNotification(
                        requireContext(),
                        messages
                    )
                }
            }
            val dueDate = transactionData!!.dueDateTransaction as Long
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val dueDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(dueDate), ZoneId.systemDefault())
                val currentTime = LocalDateTime.now()
                val monthsPassed = ChronoUnit.MONTHS.between(dueDateTime, currentTime)
                checkMonth(monthsPassed)
            } else {
                val currentTime = Calendar.getInstance().timeInMillis
                val dueDateCalendar = Calendar.getInstance()
                dueDateCalendar.timeInMillis = dueDate
                val currentCalendar = Calendar.getInstance()
                currentCalendar.timeInMillis = currentTime

                val monthsPassed = 12 * (currentCalendar.get(Calendar.YEAR) - dueDateCalendar.get(Calendar.YEAR)) +
                        (currentCalendar.get(Calendar.MONTH) - dueDateCalendar.get(Calendar.MONTH))
                checkMonth(monthsPassed.toLong())
            }
        }
    }

    private fun checkMonth(months: Long) {
        if (months >= 1) {
            if (transactionData!!.stateTransaction == false) {
                messages.add(
                    resources.getString(
                        R.string.messagePassInfo,
                        transactionData!!.name,
                        months.toString()
                    )
                )
                showCurrentNotification(
                    requireContext(),
                    messages
                )
            }
        }
    }

    private fun showCurrentNotification(context: Context, messages: ArrayList<String>) {
        val sb = java.lang.StringBuilder()
        messages.forEachIndexed { index, value -> sb.append("${index + 1}. $value\n") }
        val joinedStringMessage = sb.toString().removeSuffix(", ")

        val intent = Intent(requireContext(), DetailMessagesActivity::class.java)

        var title = resources.getString(R.string.info_not_yet_full)

        if (messages.isEmpty()) {
            intent.putExtra("messages", getString(R.string.empty_data))
            title = getString(R.string.message_is_empty)
            startNotification(intent, context, title, getString(R.string.empty_data))
        } else {
            intent.putExtra("messages", joinedStringMessage)
            startNotification(intent, context, title, joinedStringMessage)
        }
    }

    private fun startNotification(
        intent: Intent,
        context: Context,
        title: String,
        joinedStringMessage: String
    ) {
        val pendingIntent = PendingIntent.getActivity(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val mNotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val mBuilder = NotificationCompat.Builder(requireContext(), CHANNEL_NOTIFICATION_INSTANT_ID)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_notification))
            .setContentTitle(title)
            .setContentText(joinedStringMessage)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setStyle(NotificationCompat.BigTextStyle().bigText(joinedStringMessage))
            .setAutoCancel(true)

        /*
        Untuk android Oreo ke atas perlu menambahkan notification channel
        */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            /* Create or update. */
            val channel = NotificationChannel(
                CHANNEL_NOTIFICATION_INSTANT_ID,
                CHANNEL_NOTIFICATION_INSTANT_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = CHANNEL_NOTIFICATION_INSTANT_NAME
            mBuilder.setChannelId(CHANNEL_NOTIFICATION_INSTANT_ID)
            mNotificationManager.createNotificationChannel(channel)
        }

        val notification = mBuilder.build()

        mNotificationManager.notify(NOTIFICATION_INSTANT_ID, notification)
    }

    private fun showInAdapter(showRevenue: Boolean) {
        val mAdapter = TransactionAdapter()
        mAdapter.submitList(transactionList)

        binding.rvTransaction.adapter = mAdapter

        mAdapter.setOnItemClickListener(object : TransactionAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                val intent = Intent(
                    this@TransactionFragment.activity,
                    TransactionDetailsActivity::class.java
                )

                intent.putExtra(TRANSACTION_ID_EXTRA, transactionList[position].id)
                intent.putExtra(NAME_EXTRA, transactionList[position].name)
                intent.putExtra(DATE_EXTRA, transactionList[position].dueDateTransaction)
                intent.putExtra(DATE_INVERTED_EXTRA, transactionList[position].invertedDate)
                intent.putExtra(AMOUNT_EXTRA, transactionList[position].paymentAmount)
                intent.putExtra(STATUS_EXTRA, transactionList[position].stateTransaction)
                intent.putExtra(WHATS_APP_EXTRA, transactionList[position].whatsAppNumber)
                intent.putExtra(AMOUNT_LEFT_EXTRA, transactionList[position].amountLeft)
                intent.putExtra(AMOUNT_OVER_EXTRA, transactionList[position].amountOver)
                intent.putExtra(AMOUNT_PAYED, transactionList[position].amountPayed)

                startActivity(intent)
            }
        })

        mAdapter.setOnLongItemClickListener(object : TransactionAdapter.OnLongItemClickListener {
            override fun onLongItemClick(position: Int) {

                val idTransaction = transactionList[position].id.toString()
                val name = transactionList[position].name.toString()
                val dueDateTransaction = transactionList[position].dueDateTransaction as Long
                val invertedDate = transactionList[position].invertedDate as Long
                val paymentAmount = transactionList[position].paymentAmount
                val whatsAppNumber = transactionList[position].whatsAppNumber.toString()
                val stateTransaction = transactionList[position].stateTransaction as Boolean

                showDialog(idTransaction, name, dueDateTransaction, invertedDate, paymentAmount, whatsAppNumber, stateTransaction)
            }
        })

        binding.rvTransaction.visibility = View.VISIBLE

        if (showRevenue) {
            showTotalRevenue()
        }
    }

    private fun showDialog(
        idTransaction: String,
        name: String,
        dueDateTransaction: Long,
        invertedDate: Long,
        paymentAmount: Double?,
        whatsAppNumber: String,
        stateTransaction: Boolean
    ) {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = MarkDialogBinding.inflate(layoutInflater)
        builder.setView(dialogView.root)

        val markDialog = builder.create()

        dialogView.tvTitle.text = getString(R.string.choose_action_info)
        dialogView.infoChooseItem.text = getString(R.string.you_choose_item_info, name, formatRupiah?.format(paymentAmount)?.replace(",00", ""))
        dialogView.btnCancel.setOnClickListener {
            markDialog.dismiss()
        }
        dialogView.btnMark.setOnClickListener {
            markAsDone(idTransaction, name, dueDateTransaction, invertedDate, paymentAmount, whatsAppNumber, stateTransaction, markDialog, dialogView)
        }
        markDialog.show()
    }

    private fun markAsDone(
        idTransaction: String,
        name: String,
        dueDateTransaction: Long,
        invertedDate: Long,
        paymentAmount: Double?,
        whatsAppNumber: String,
        stateTransaction: Boolean,
        markDialog: AlertDialog,
        dialogView: MarkDialogBinding
    ) {
        dialogView.progressBar.visibility = View.VISIBLE
        val user = Firebase.auth.currentUser
        val uid = user?.uid

        val broadcastReceiver = MonthlyNotification()

        val calendar: Calendar = Calendar.getInstance()
        val dateFromLong = Date(dueDateTransaction)
        calendar.time = dateFromLong
        calendar.add(Calendar.MONTH, 1)
        val nextMonth: Date = calendar.time

        val invertedDateAddOneMonth = nextMonth.time * -1

        if (uid != null) {
            val dbRef = FirebaseDatabase.getInstance().getReference(uid)
            val transactionInfo = TransactionModel(idTransaction, name, whatsAppNumber, paymentAmount, dueDateTransaction, true, invertedDate, 0.0, 0.0, paymentAmount)

            createAddNextMonthTransaction(
                dbRef,
                name,
                whatsAppNumber,
                paymentAmount,
                nextMonth,
                invertedDateAddOneMonth,
                stateTransaction,
                broadcastReceiver
            )

            dbRef.child(idTransaction).setValue(transactionInfo)
                .addOnCompleteListener {
                    dialogView.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), getString(R.string.success_to_mark), Toast.LENGTH_SHORT).show()
                    if (isSearched) {
                        querySearch.removeEventListener(valueEventListenerSearch)
                        getSearchData(name)
                    }
                    markDialog.dismiss()
                }
                .addOnFailureListener { err ->
                    dialogView.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error ${err.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun createAddNextMonthTransaction(
        dbRef: DatabaseReference,
        name: String,
        whatsAppNumber: String,
        paymentAmount: Double?,
        nextMonth: Date,
        invertedDateAddOneMonth: Long,
        stateTransaction: Boolean,
        broadcastReceiver: MonthlyNotification
    ) {

        if (!stateTransaction) {
            val newTransactionID = dbRef.push().key!! + "0"
            val transactionAddOneMonth = TransactionModel(
                newTransactionID,
                name,
                whatsAppNumber,
                paymentAmount,
                nextMonth.time,
                false,
                invertedDateAddOneMonth,
                paymentAmount,
                0.0,
                0.0
            )
            broadcastReceiver.setMonthlyNotification(requireContext(), name, paymentAmount!!.toDouble(), nextMonth.time)
            dbRef.child(newTransactionID).setValue(transactionAddOneMonth)
        }
    }

    private fun showTotalRevenue() {
        totalRevenue =
            transactionList.fold(0.0) { acc, amountPayed -> acc + amountPayed.amountPayed as Double }
        totalTransaction = transactionList.size

        binding.numberOfRevenue.text = formatRupiah?.format(totalRevenue)?.replace(",00", "")
        binding.tvTotalClient.text =
            resources.getString(R.string.total_transaction, totalTransaction.toString())
        totalTransaction = 0
    }

    private fun getRangeDate(rangeType: Int, month: Int) {
        val currentDate = Date()
        val cal: Calendar = Calendar.getInstance(TimeZone.getDefault())
        cal.time = currentDate

        val startDay = cal.getActualMinimum(rangeType)
        cal.set(selectedYear.toInt(), month, startDay)
        val startDate = cal.time
        dateStart = startDate.time

        val endDay = cal.getActualMaximum(rangeType)
        cal.set(selectedYear.toInt(), month, endDay)
        val endDate = cal.time
        dateEnd = endDate.time
    }

    private fun showUserName() {
        user?.reload()

        email = user!!.email
        username = user.displayName

        name = if (username == null || username == "") {
            splitValue = email?.split("@")
            splitValue?.get(0).toString()
        } else {
            username
        }

        binding.tvUsername.text = resources.getString(R.string.greetings_name, name!!.split(" ")[0])
    }

    override fun onResume() {
        super.onResume()
        queryGetTransactionData.removeEventListener(valueEventListenerGetTransactionData)
        if (isSearched) {
            querySearch.removeEventListener(valueEventListenerSearch)
            getSearchData(searchText.toString())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isSearched) {
            querySearch.removeEventListener(valueEventListenerSearch)
        }
        try {
            dbRef.removeEventListener(valueEventListenerGetTransactionData)
            queryGetTransactionData.removeEventListener(valueEventListenerGetTransactionData)
        } catch (e: Exception) {
            Log.e("TransactionFragment", "onDestroy: ${e.message}")
        }
        transactionData = null
        calendar = null
        dateFormat = null
        formatRupiah = null
        email = null
        username = null
        name = null
        splitValue = null
        _binding = null
    }


}