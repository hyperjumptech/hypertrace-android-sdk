package tech.hyperjump.hypertrace.streetpassdebug

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import tech.hyperjump.hypertrace.databinding.StreetPassRecordItemBinding
import tech.hyperjump.hypertrace.streetpass.persistence.StreetPassRecord

class StreetPassRecordAdapter(private val onListUpdated: () -> Unit)
    : ListAdapter<StreetPassRecordAdapter.Info, StreetPassRecordAdapter.ViewHolder>(DIFF_UTIL) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = StreetPassRecordItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = getItem(position)
        holder.viewBinding.apply {
            tvDate.text = "Time: ${record.date}"
            tvV.text = "V: ${record.data.v}"
            tvMsg.text = "Msg: ${record.data.msg}"
            tvOrg.text = "Org: ${record.data.org}"
            tvModelP.text = "Model P: ${record.data.modelP}"
            tvModelC.text = "Model C: ${record.data.modelC}"
            tvRssi.text = "RSSI: ${record.data.rssi}"
            tvTxPower.text = "Tx Power: ${record.data.txPower}"
        }
    }

    override fun onCurrentListChanged(previousList: MutableList<Info>, currentList: MutableList<Info>) {
        super.onCurrentListChanged(previousList, currentList)
        onListUpdated()
    }

    class ViewHolder(val viewBinding: StreetPassRecordItemBinding)
        : RecyclerView.ViewHolder(viewBinding.root)

    class Info(val data: StreetPassRecord, val date: String)

    companion object {
        private val DIFF_UTIL = object : DiffUtil.ItemCallback<Info>() {
            override fun areItemsTheSame(oldItem: Info, newItem: Info): Boolean {
                return oldItem.data.id == newItem.data.id
            }

            override fun areContentsTheSame(oldItem: Info, newItem: Info): Boolean {
                return oldItem.data == newItem.data
            }

        }
    }
}
