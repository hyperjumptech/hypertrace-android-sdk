package tech.hyperjump.hypertrace.streetpassdebug

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.hyperjump.hypertrace.HyperTraceSdk
import tech.hyperjump.hypertrace.databinding.ActivityStreetPassDebugBinding
import tech.hyperjump.hypertrace.streetpass.persistence.StreetPassRecordDatabase
import java.text.SimpleDateFormat
import java.util.*

class StreetPassDebugActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityStreetPassDebugBinding
    private var streetPassRecordAdapter: StreetPassRecordAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityStreetPassDebugBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        if (HyperTraceSdk.CONFIG.debug) showStreetPassRecords()
    }

    private fun showStreetPassRecords() {
        streetPassRecordAdapter = StreetPassRecordAdapter {
            viewBinding.rvStreetPass.smoothScrollToPosition(0)
        }
        viewBinding.rvStreetPass.apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, RecyclerView.VERTICAL))
            adapter = streetPassRecordAdapter
        }
        StreetPassRecordDatabase.getDatabase(applicationContext)
                .recordDao()
                .getRecords()
                .observe(this) {
                    if (it != null) {
                        lifecycleScope.launchWhenResumed {
                            val infos = withContext(Dispatchers.Default) {
                                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
                                it.map { record ->
                                    StreetPassRecordAdapter.Info(record, formatter.format(record.timestamp))
                                }
                            }
                            streetPassRecordAdapter?.submitList(infos)
                        }
                    }
                }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewBinding.rvStreetPass.adapter = null
        streetPassRecordAdapter = null
    }
}
