package com.example.twittermemoapp

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputEditText
import io.noties.markwon.Markwon
import io.noties.markwon.editor.MarkwonEditor

class MemoDialogFragment : DialogFragment() {
    companion object {
        private const val ARG_MEMO = "memo"

        fun newInstance(editor: MarkwonEditor, markwon: Markwon, memo: Memo? = null): MemoDialogFragment {
            return MemoDialogFragment().apply {
                this.editor = editor
                this.markwon = markwon
                arguments = Bundle().apply {
                    memo?.let { putSerializable(ARG_MEMO, it) }
                }
            }
        }
    }

    private lateinit var editor: MarkwonEditor
    private lateinit var markwon: Markwon
    private var memo: Memo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        memo = arguments?.getSerializable(ARG_MEMO) as? Memo
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity()
        val builder = AlertDialog.Builder(activity)
        val view = layoutInflater.inflate(R.layout.dialog_memo, null)
        val editText = view.findViewById<TextInputEditText>(R.id.memoEditText)
        val previewText = view.findViewById<TextView>(R.id.previewText)

        // 既存のメモがある場合はセット
        memo?.let {
            editText.setText(it.content)
        }

        // プレビューの更新
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                markwon.setMarkdown(previewText, s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        return builder
            .setView(view)
            .setTitle(if (memo == null) "新規メモ" else "メモを編集")
            .setPositiveButton(if (memo == null) "作成" else "更新") { _, _ ->
                val content = editText.text.toString()
                if (content.isNotEmpty()) {
                    if (memo == null) {
                        (activity as MainActivity).addMemo(content)
                    } else {
                        (activity as MainActivity).updateMemo(memo!!, content)
                    }
                }
            }
            .setNegativeButton("キャンセル", null)
            .create()
    }
}