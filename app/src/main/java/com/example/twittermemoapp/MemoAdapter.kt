package com.example.twittermemoapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import io.noties.markwon.Markwon

class MemoAdapter(
    private val memos: List<Memo>,
    private val markwon: Markwon,
    private val onMemoClick: (Memo) -> Unit,
    private val onMemoLongClick: (Memo) -> Unit = {},
    private val onSelectionChanged: (Memo, Boolean) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<MemoAdapter.ViewHolder>() {

    private val selectedMemos = mutableSetOf<Long>()
    private var isMultiSelectMode = false

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val content: TextView = view.findViewById(R.id.memoContent)
        val timestamp: TextView = view.findViewById(R.id.timestamp)
        val cardView: MaterialCardView = view.findViewById(R.id.memoCard)
        val checkbox: CheckBox = view.findViewById(R.id.selectionCheckbox)
        val defaultIndicator: TextView = view.findViewById(R.id.defaultIndicator)
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

        // 選択状態の管理
        val isSelected = selectedMemos.contains(memo.id)
        holder.checkbox.isChecked = isSelected
        holder.checkbox.visibility = if (isMultiSelectMode) View.VISIBLE else View.GONE
        
        // デフォルトメモ表示
        val appPrefs = AppPreferences(holder.itemView.context)
        holder.defaultIndicator.visibility = if (appPrefs.defaultMemoId == memo.id) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // カードの背景色を選択状態に応じて変更
        holder.cardView.isChecked = isSelected

        // クリックリスナー
        holder.cardView.setOnClickListener {
            if (isMultiSelectMode) {
                toggleSelection(memo, holder)
            } else {
                onMemoClick(memo)
            }
        }

        // ロングクリックリスナー
        holder.cardView.setOnLongClickListener {
            if (!isMultiSelectMode) {
                startMultiSelectMode()
                toggleSelection(memo, holder)
                onMemoLongClick(memo)
            }
            true
        }

        // チェックボックスクリックリスナー
        holder.checkbox.setOnClickListener {
            toggleSelection(memo, holder)
        }
    }

    private fun toggleSelection(memo: Memo, holder: ViewHolder) {
        if (selectedMemos.contains(memo.id)) {
            selectedMemos.remove(memo.id)
            holder.checkbox.isChecked = false
            holder.cardView.isChecked = false
        } else {
            selectedMemos.add(memo.id)
            holder.checkbox.isChecked = true
            holder.cardView.isChecked = true
        }
        onSelectionChanged(memo, selectedMemos.contains(memo.id))
        
        // 選択が全て解除されたら通常モードに戻る
        if (selectedMemos.isEmpty()) {
            exitMultiSelectMode()
        }
    }

    fun startMultiSelectMode() {
        isMultiSelectMode = true
        notifyDataSetChanged()
    }

    fun exitMultiSelectMode() {
        isMultiSelectMode = false
        selectedMemos.clear()
        notifyDataSetChanged()
    }

    fun getSelectedMemos(): List<Memo> {
        return memos.filter { selectedMemos.contains(it.id) }
    }

    fun isInMultiSelectMode(): Boolean = isMultiSelectMode

    fun getSelectedCount(): Int = selectedMemos.size

    override fun getItemCount() = memos.size
}