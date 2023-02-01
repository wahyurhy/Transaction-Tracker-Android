package com.wahyurhy.transactiontracker.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.wahyurhy.transactiontracker.R
import com.wahyurhy.transactiontracker.adapter.TransactionAdapter
import com.wahyurhy.transactiontracker.data.source.local.model.TransactionModel
import com.wahyurhy.transactiontracker.databinding.FragmentTransactionBinding
import com.wahyurhy.transactiontracker.ui.details.TransactionDetailsActivity
import com.wahyurhy.transactiontracker.utils.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class TransactionFragment : Fragment() {

    private val user = Firebase.auth.currentUser
    private lateinit var dbRef: DatabaseReference
    private var selectedTimeSpan: String = sdf.format(currentYear) // default year
    private var selectedShowStatus: String =
        arrayOf(R.array.filter_sort_by_status)[0].toString() // default all time
    private var year: String? = ""
    private var getYear: Int = 0
    private var totalRevenue = 0.0
    private var totalTransaction = 0
    private var selectedYear = ""
    private var isSearched = false
    private val searchText = StringBuilder()
    private lateinit var transactionList: ArrayList<TransactionModel>
    private lateinit var valueEventListenerGetTransactionData: ValueEventListener
    private lateinit var valueEventListenerSearch: ValueEventListener
    private lateinit var queryGetTransactionData: Query
    private lateinit var querySearch: Query
    private var transactionData: TransactionModel? = null
    private var calendar: Calendar? = null
    private var dateFormat: SimpleDateFormat? = null
    private var localeID: Locale? = null
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
        // Inflate the layout for this fragment
        _binding = FragmentTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        calendar = Calendar.getInstance()
        dateFormat = SimpleDateFormat("yyyy", Locale.getDefault())
        localeID = Locale("in", "ID")
        formatRupiah = NumberFormat.getCurrencyInstance(localeID!!)

        showUserName()

        setCurrentYear()

        visibilityOptions()

        // -- Recycler View transaction items --
        binding.rvTransaction.layoutManager = LinearLayoutManager(this.activity)
        binding.rvTransaction.setHasFixedSize(true)

        transactionList = arrayListOf()

        getTransactionData()

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
                                            transactionData = transactionSnap.getValue(TransactionModel::class.java)

                                            calendar?.timeInMillis = transactionData!!.dueDateTransaction as Long

                                            year = dateFormat?.format(calendar!!.time)

                                            if (year == selectedYear) {
                                                transactionList.add(transactionData!!)
                                            }
                                        }
                                    }
                                    resources.getStringArray(R.array.filter_sort_by_status)[1].toString() -> {
                                        for (transactionSnap in snapshot.children) {
                                            transactionData = transactionSnap.getValue(TransactionModel::class.java)

                                            calendar?.timeInMillis = transactionData!!.dueDateTransaction as Long

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
                                            transactionData = transactionSnap.getValue(TransactionModel::class.java)

                                            calendar?.timeInMillis = transactionData!!.dueDateTransaction as Long

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
                            TODO("Not yet implemented")
                        }
                    }

                    getSearchData(newText)
                    searchText.replace(0, searchText.length, newText)
                    isSearched = true
                } else {
                    getTransactionData()
                }
                return true
            }
        })

        binding.swipeRefresh.setOnRefreshListener {
            getTransactionData()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun getSearchData(text: String) {
        querySearch = dbRef.orderByChild("name").startAt(text)
        querySearch.addListenerForSingleValueEvent(valueEventListenerSearch)
    }

    private fun setCurrentYear() {
        for (i in 0..11){
            when (selectedTimeSpan) {
                resources.getStringArray(R.array.filter_sort_by_periode)[i].toString() -> getYear = i + 1
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
                                binding.tvDesc.text = getString(R.string.info_revenue, resources.getStringArray(R.array.filter_sort_by_periode)[i].toString())
                                selectedTimeSpan = resources.getStringArray(R.array.filter_sort_by_periode)[i].toString()
                                selectedYear = resources.getStringArray(R.array.filter_sort_by_periode)[i].toString()
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
        totalRevenue = 0.0
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

                                calendar?.timeInMillis = transactionData!!.dueDateTransaction as Long

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

                                calendar?.timeInMillis = transactionData!!.dueDateTransaction as Long

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

                                calendar?.timeInMillis = transactionData!!.dueDateTransaction as Long

                                year = dateFormat?.format(calendar!!.time)

                                if (!transactionData!!.stateTransaction!!) {
                                    if (year == selectedYear) {
                                        transactionList.add(transactionData!!)
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
            }

            override fun onCancelled(error: DatabaseError) {
                print("Listener was cancelled")
                Log.d("TransactionFragment", "Listener was cancelled ${error.message}")
            }
        }
        queryGetTransactionData.addValueEventListener(valueEventListenerGetTransactionData)

    }

    private fun showInAdapter(showRevenue: Boolean) {
        val mAdapter = TransactionAdapter(transactionList)

        binding.rvTransaction.adapter = mAdapter

        mAdapter.setOnItemClickListener(object : TransactionAdapter.onItemClickListener {
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

        binding.rvTransaction.visibility = View.VISIBLE

        if (showRevenue) {
            showTotalRevenue()
        }
    }

    private fun showTotalRevenue() {
        for (position in 0 until transactionList.size) {
            totalRevenue += transactionList[position].amountPayed!!
            totalTransaction = transactionList.size
        }

        binding.numberOfRevenue.text = formatRupiah?.format(totalRevenue)?.replace(",00", "")
        binding.tvTotalClient.text =
            resources.getString(R.string.total_transaction, totalTransaction.toString())
        totalRevenue = 0.0
        totalTransaction = 0
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

        binding.tvUsername.text = resources.getString(R.string.greetings_name, name)
    }

    override fun onResume() {
        super.onResume()
        dbRef.removeEventListener(valueEventListenerGetTransactionData)
        queryGetTransactionData.removeEventListener(valueEventListenerGetTransactionData)
        if (isSearched) {
            querySearch.removeEventListener(valueEventListenerSearch)
            getSearchData(searchText.toString())
        } else {
            getTransactionData()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isSearched) {
            querySearch.removeEventListener(valueEventListenerSearch)
        }
        dbRef.removeEventListener(valueEventListenerGetTransactionData)
        queryGetTransactionData.removeEventListener(valueEventListenerGetTransactionData)
        transactionData = null
        calendar = null
        dateFormat = null
        localeID = null
        formatRupiah = null
        email = null
        username = null
        name = null
        splitValue = null
    }


}