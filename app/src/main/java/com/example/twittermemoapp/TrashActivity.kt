package com.example.twittermemoapp

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin

class TrashActivity : AppCompatActivity() {
    private val deletedMemos = mutableListOf<Memo>()
    private lateinit var adapter: TrashAdapter
    private lateinit var markwon: Markwon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)

        // Markwonの初期化
        markwon = Markwon.builder(this)
            .usePlugin(HtmlPlugin.create())
            .build()

        setupToolbar()
        setupRecyclerView()
        loadDeletedMemos()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "ゴミ箱"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        adapter = TrashAdapter(
            deletedMemos,
            markwon,
            onRestoreClick = { memo -> restoreMemo(memo) },
            onPermanentDeleteClick = { memo -> showPermanentDeleteDialog(memo) }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun loadDeletedMemos() {
        // MainActivity から削除されたメモを取得
        val mainActivity = MainActivity.getInstance()
        mainActivity?.let {
            deletedMemos.clear()
            deletedMemos.addAll(it.getDeletedMemos())
            adapter.notifyDataSetChanged()
        }
    }

    private fun restoreMemo(memo: Memo) {
        AlertDialog.Builder(this)
            .setTitle("メモの復元")
            .setMessage("このメモを復元しますか？")
            .setPositiveButton("復元") { _, _ ->
                MainActivity.getInstance()?.restoreMemo(memo)
                loadDeletedMemos()
                Toast.makeText(this, "メモを復元しました", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun showPermanentDeleteDialog(memo: Memo) {
        AlertDialog.Builder(this)
            .setTitle("完全削除")
            .setMessage("このメモを完全に削除しますか？この操作は取り消せません。")
            .setPositiveButton("削除") { _, _ ->
                MainActivity.getInstance()?.permanentlyDeleteMemo(memo)
                loadDeletedMemos()
                Toast.makeText(this, "メモを完全に削除しました", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.trash_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_clear_expired -> {
                clearExpiredMemos()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun clearExpiredMemos() {
        AlertDialog.Builder(this)
            .setTitle("期限切れメモの削除")
            .setMessage("1ヶ月以上経過したメモを完全に削除しますか？")
            .setPositiveButton("削除") { _, _ ->
                MainActivity.getInstance()?.clearExpiredMemos()
                loadDeletedMemos()
                Toast.makeText(this, "期限切れメモを削除しました", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadDeletedMemos()
    }
}