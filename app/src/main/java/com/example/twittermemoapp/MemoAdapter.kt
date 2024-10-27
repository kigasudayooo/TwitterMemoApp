package com.example.twittermemoapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import io.noties.markwon.Markwon

class MemoAdapter(
    private val memos: List<Memo>,
    private val markwon: Markwon,
    private val onMemoClick: (Memo) -> Unit  // クリックリスナーのみ残す
) : RecyclerView.Adapter<MemoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val content: TextView = view.findViewById(R.id.memoContent)
        val timestamp: TextView = view.findViewById(R.id.timestamp)
        val cardView: MaterialCardView = view.findViewById(R.id.memoCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.memo_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val memo = memos[position]
        // マークダウンとしてテキストを設定
        markwon.setMarkdown(holder.content, memo.content)
        holder.timestamp.text = memo.timestamp

        // クリックリスナーを設定
        holder.cardView.setOnClickListener {
            onMemoClick(memo)
        }
    }

    override fun getItemCount() = memos.size
}