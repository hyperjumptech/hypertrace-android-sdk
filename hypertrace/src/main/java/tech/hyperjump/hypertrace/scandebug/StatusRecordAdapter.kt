package tech.hyperjump.hypertrace.scandebug

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import tech.hyperjump.hypertrace.databinding.StatusRecordItemBinding
import io.bluetrace.opentrace.status.persistence.StatusRecord

class StatusRecordAdapter(private val onListUpdated: () -> Unit) : ListAdapter<StatusRecordAdapter.ReadableInfo, StatusRecordAdapter.ViewHolder>(DIFF_UTIL) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = StatusRecordItemBinding.inflate(inflater, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.viewBinding.tvStatus.text = getItem(position).data.msg
        holder.viewBinding.tvDate.text = getItem(position).date
    }

    override fun onCurrentListChanged(previousList: MutableList<ReadableInfo>, currentList: MutableList<ReadableInfo>) {
        super.onCurrentListChanged(previousList, currentList)
        onListUpdated()
    }

    class ViewHolder(val viewBinding: StatusRecordItemBinding)
        : RecyclerView.ViewHolder(viewBinding.root)

    class ReadableInfo(val data: StatusRecord, val date: String)

    companion object {
        private val DIFF_UTIL = object : DiffUtil.ItemCallback<ReadableInfo>() {
            override fun areItemsTheSame(oldItem: ReadableInfo, newItem: ReadableInfo): Boolean {
                return oldItem.data.id == newItem.data.id
            }

            override fun areContentsTheSame(oldItem: ReadableInfo, newItem: ReadableInfo): Boolean {
                return oldItem.data == newItem.data
            }
        }
    }
}
