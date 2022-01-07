package tech.hyperjump.hypertrace.scandebug

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.hyperjump.hypertrace.HyperTraceSdk
import tech.hyperjump.hypertrace.databinding.ActivityScanDebugBinding
import tech.hyperjump.hypertrace.streetpass.persistence.StreetPassRecordDatabase
import java.text.SimpleDateFormat
import java.util.*

class ScanDebugActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanDebugBinding
    private var statusRecordAdapter: StatusRecordAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanDebugBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (HyperTraceSdk.CONFIG.debug) showStatusRecords()
    }

    private fun showStatusRecords() {
        statusRecordAdapter = StatusRecordAdapter {
            binding.rvStatusRecords.smoothScrollToPosition(0)
        }
        binding.rvStatusRecords.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = statusRecordAdapter
            addItemDecoration(DividerItemDecoration(context, RecyclerView.VERTICAL))
        }
        StreetPassRecordDatabase.getDatabase(applicationContext)
                .statusDao()
                .getRecords()
                .observe(this) {
                    if (it != null) {
                        lifecycleScope.launchWhenResumed {
                            val infos = withContext(Dispatchers.Default) {
                                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
                                it.map { record ->
                                    StatusRecordAdapter.ReadableInfo(data = record, date = formatter.format(record.timestamp))
                                }
                            }
                            statusRecordAdapter?.submitList(infos)
                        }
                    }
                }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.rvStatusRecords.adapter = null
        statusRecordAdapter = null
    }
}
