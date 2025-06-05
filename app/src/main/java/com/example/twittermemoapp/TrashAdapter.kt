package com.example.twittermemoapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon
import java.text.SimpleDateFormat
import java.util.*

class TrashAdapter(
    private val memos: List<Memo>,
    private val markwon: Markwon,
    private val onRestoreClick: (Memo) -> Unit,
    private val onPermanentDeleteClick: (Memo) -> Unit
) : RecyclerView.Adapter<TrashAdapter.TrashViewHolder>() {

    class TrashViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val contentText: TextView = view.findViewById(R.id.contentText)
        val timestampText: TextView = view.findViewById(R.id.timestampText)
        val deletedAtText: TextView = view.findViewById(R.id.deletedAtText)
        val restoreButton: Button = view.findViewById(R.id.restoreButton)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
        val expiredText: TextView = view.findViewById(R.id.expiredText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrashViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trash_memo, parent, false)
        return TrashViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrashViewHolder, position: Int) {
        val memo = memos[position]
        
        // コンテンツ表示（Markdownレンダリング）
        markwon.setMarkdown(holder.contentText, memo.content)
        
        // タイムスタンプ表示
        holder.timestampText.text = "作成: ${memo.timestamp}"
        
        // 削除日時表示
        memo.deletedAt?.let { deletedTime ->
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            holder.deletedAtText.text = "削除: ${formatter.format(Date(deletedTime))}"
            holder.deletedAtText.visibility = View.VISIBLE
        } ?: run {
            holder.deletedAtText.visibility = View.GONE
        }
        
        // 期限切れ表示
        if (memo.canPermanentlyDelete()) {
            holder.expiredText.visibility = View.VISIBLE
            holder.expiredText.text = "期限切れ（完全削除可能）"
        } else {
            holder.expiredText.visibility = View.GONE
        }
        
        // ボタンクリックリスナー
        holder.restoreButton.setOnClickListener {
            onRestoreClick(memo)
        }
        
        holder.deleteButton.setOnClickListener {
            onPermanentDeleteClick(memo)
        }
    }

    override fun getItemCount() = memos.size
}