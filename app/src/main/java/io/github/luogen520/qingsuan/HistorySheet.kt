package io.github.luogen520.qingsuan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog

/** 历史记录底部弹层，点一条把结果拿回来接着算。 */
class HistorySheet(
    context: android.content.Context,
    private val entries: MutableList<HistoryEntry>,
    private val onPick: (HistoryEntry) -> Unit,
    private val onClear: () -> Unit,
) : BottomSheetDialog(context) {

    private lateinit var adapter: Adapter

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.sheet_history, null)
        setContentView(view)

        val list = view.findViewById<RecyclerView>(R.id.listHistory)
        val empty = view.findViewById<TextView>(R.id.txtEmpty)
        adapter = Adapter()
        list.layoutManager = LinearLayoutManager(context)
        list.adapter = adapter
        empty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE

        view.findViewById<TextView>(R.id.btnClearHistory).setOnClickListener {
            entries.clear()
            onClear()
            dismiss()
        }
    }

    private inner class Adapter : RecyclerView.Adapter<VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
            return VH(v)
        }

        override fun getItemCount() = entries.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val e = entries[position]
            holder.expr.text = e.expression
            holder.result.text = e.result
            holder.itemView.setOnClickListener {
                onPick(e)
                dismiss()
            }
        }
    }

    private class VH(v: View) : RecyclerView.ViewHolder(v) {
        val expr: TextView = v.findViewById(R.id.txtItemExpr)
        val result: TextView = v.findViewById(R.id.txtItemResult)
    }
}
